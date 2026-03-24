const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { OAuth2Client } = require('google-auth-library');
const twilio = require('twilio');
const { body, validationResult } = require('express-validator');
const { v4: uuidv4 } = require('uuid');
const { authenticate, prisma } = require('../middleware/auth');

const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

let twilioClient = null;
if (process.env.TWILIO_ACCOUNT_SID && process.env.TWILIO_AUTH_TOKEN) {
  twilioClient = twilio(process.env.TWILIO_ACCOUNT_SID, process.env.TWILIO_AUTH_TOKEN);
}

function generateTokens(userId) {
  const accessToken = jwt.sign(
    { userId },
    process.env.JWT_SECRET,
    { expiresIn: process.env.JWT_EXPIRES_IN || '15m' }
  );
  const refreshToken = jwt.sign(
    { userId },
    process.env.JWT_REFRESH_SECRET,
    { expiresIn: process.env.JWT_REFRESH_EXPIRES_IN || '7d' }
  );
  return { accessToken, refreshToken };
}

function generateUsername(name) {
  const base = name.toLowerCase().replace(/[^a-z0-9]/g, '');
  return base + Math.floor(Math.random() * 9999);
}

// Register with email
router.post('/register',
  [
    body('email').isEmail().normalizeEmail(),
    body('password').isLength({ min: 8 }),
    body('name').trim().notEmpty().isLength({ max: 50 }),
  ],
  async (req, res, next) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const { email, password, name } = req.body;

      const existing = await prisma.user.findUnique({ where: { email } });
      if (existing) {
        return res.status(409).json({ error: 'Email already registered' });
      }

      const passwordHash = await bcrypt.hash(password, 12);
      let username = generateUsername(name);

      // Ensure username uniqueness
      while (await prisma.user.findUnique({ where: { username } })) {
        username = generateUsername(name);
      }

      const user = await prisma.user.create({
        data: { email, passwordHash, name, username },
        select: { id: true, email: true, username: true, name: true, avatarUrl: true },
      });

      const { accessToken, refreshToken } = generateTokens(user.id);

      await prisma.refreshToken.create({
        data: {
          token: refreshToken,
          userId: user.id,
          expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
        },
      });

      res.status(201).json({ user, accessToken, refreshToken });
    } catch (error) {
      next(error);
    }
  }
);

// Login with email
router.post('/login',
  [
    body('email').isEmail().normalizeEmail(),
    body('password').notEmpty(),
  ],
  async (req, res, next) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const { email, password } = req.body;

      const user = await prisma.user.findUnique({ where: { email } });
      if (!user || !user.passwordHash) {
        return res.status(401).json({ error: 'Invalid credentials' });
      }

      const valid = await bcrypt.compare(password, user.passwordHash);
      if (!valid) {
        return res.status(401).json({ error: 'Invalid credentials' });
      }

      const { accessToken, refreshToken } = generateTokens(user.id);

      await prisma.refreshToken.create({
        data: {
          token: refreshToken,
          userId: user.id,
          expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
        },
      });

      // Update online status
      await prisma.user.update({
        where: { id: user.id },
        data: { isOnline: true },
      });

      const { passwordHash, ...userWithoutPassword } = user;
      res.json({ user: userWithoutPassword, accessToken, refreshToken });
    } catch (error) {
      next(error);
    }
  }
);

// Google Sign-In
router.post('/google', async (req, res, next) => {
  try {
    const { idToken } = req.body;
    if (!idToken) {
      return res.status(400).json({ error: 'ID token is required' });
    }

    const ticket = await googleClient.verifyIdToken({
      idToken,
      audience: process.env.GOOGLE_CLIENT_ID,
    });

    const payload = ticket.getPayload();
    const { sub: googleId, email, name, picture } = payload;

    let user = await prisma.user.findFirst({
      where: { OR: [{ googleId }, { email }] },
    });

    if (!user) {
      let username = generateUsername(name);
      while (await prisma.user.findUnique({ where: { username } })) {
        username = generateUsername(name);
      }

      user = await prisma.user.create({
        data: {
          googleId,
          email,
          name,
          username,
          avatarUrl: picture,
          isVerified: true,
        },
      });
    } else if (!user.googleId) {
      user = await prisma.user.update({
        where: { id: user.id },
        data: { googleId, avatarUrl: user.avatarUrl || picture },
      });
    }

    const { accessToken, refreshToken } = generateTokens(user.id);

    await prisma.refreshToken.create({
      data: {
        token: refreshToken,
        userId: user.id,
        expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
      },
    });

    await prisma.user.update({
      where: { id: user.id },
      data: { isOnline: true },
    });

    const { passwordHash, ...userWithoutPassword } = user;
    res.json({ user: userWithoutPassword, accessToken, refreshToken });
  } catch (error) {
    next(error);
  }
});

// Send phone OTP
router.post('/phone/send-otp',
  [body('phone').isMobilePhone()],
  async (req, res, next) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const { phone } = req.body;

      if (!twilioClient || !process.env.TWILIO_VERIFY_SERVICE_SID) {
        return res.status(503).json({ error: 'Phone verification not configured' });
      }

      await twilioClient.verify.v2
        .services(process.env.TWILIO_VERIFY_SERVICE_SID)
        .verifications.create({ to: phone, channel: 'sms' });

      res.json({ message: 'OTP sent successfully' });
    } catch (error) {
      next(error);
    }
  }
);

// Verify phone OTP
router.post('/phone/verify',
  [
    body('phone').isMobilePhone(),
    body('code').isLength({ min: 4, max: 8 }),
    body('name').optional().trim().isLength({ max: 50 }),
  ],
  async (req, res, next) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const { phone, code, name } = req.body;

      if (!twilioClient || !process.env.TWILIO_VERIFY_SERVICE_SID) {
        return res.status(503).json({ error: 'Phone verification not configured' });
      }

      const verification = await twilioClient.verify.v2
        .services(process.env.TWILIO_VERIFY_SERVICE_SID)
        .verificationChecks.create({ to: phone, code });

      if (verification.status !== 'approved') {
        return res.status(400).json({ error: 'Invalid OTP' });
      }

      let user = await prisma.user.findUnique({ where: { phone } });

      if (!user) {
        const displayName = name || `User${Math.floor(Math.random() * 9999)}`;
        let username = generateUsername(displayName);
        while (await prisma.user.findUnique({ where: { username } })) {
          username = generateUsername(displayName);
        }

        user = await prisma.user.create({
          data: { phone, name: displayName, username, isVerified: true },
        });
      } else {
        user = await prisma.user.update({
          where: { id: user.id },
          data: { isVerified: true, isOnline: true },
        });
      }

      const { accessToken, refreshToken } = generateTokens(user.id);

      await prisma.refreshToken.create({
        data: {
          token: refreshToken,
          userId: user.id,
          expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
        },
      });

      const { passwordHash, ...userWithoutPassword } = user;
      res.json({ user: userWithoutPassword, accessToken, refreshToken });
    } catch (error) {
      next(error);
    }
  }
);

// Refresh token
router.post('/refresh', async (req, res, next) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) {
      return res.status(400).json({ error: 'Refresh token required' });
    }

    const decoded = jwt.verify(refreshToken, process.env.JWT_REFRESH_SECRET);

    const storedToken = await prisma.refreshToken.findUnique({
      where: { token: refreshToken },
    });

    if (!storedToken || storedToken.userId !== decoded.userId) {
      return res.status(401).json({ error: 'Invalid refresh token' });
    }

    if (storedToken.expiresAt < new Date()) {
      await prisma.refreshToken.delete({ where: { token: refreshToken } });
      return res.status(401).json({ error: 'Refresh token expired' });
    }

    // Rotate refresh token
    await prisma.refreshToken.delete({ where: { token: refreshToken } });
    const { accessToken, refreshToken: newRefreshToken } = generateTokens(decoded.userId);

    await prisma.refreshToken.create({
      data: {
        token: newRefreshToken,
        userId: decoded.userId,
        expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
      },
    });

    res.json({ accessToken, refreshToken: newRefreshToken });
  } catch (error) {
    if (error.name === 'JsonWebTokenError' || error.name === 'TokenExpiredError') {
      return res.status(401).json({ error: 'Invalid refresh token' });
    }
    next(error);
  }
});

// Logout
router.post('/logout', authenticate, async (req, res, next) => {
  try {
    const { refreshToken } = req.body;

    if (refreshToken) {
      await prisma.refreshToken.deleteMany({ where: { token: refreshToken } });
    }

    await prisma.user.update({
      where: { id: req.user.id },
      data: { isOnline: false, lastSeen: new Date() },
    });

    res.json({ message: 'Logged out successfully' });
  } catch (error) {
    next(error);
  }
});

// Update FCM token
router.post('/fcm-token', authenticate, async (req, res, next) => {
  try {
    const { fcmToken } = req.body;
    if (!fcmToken) {
      return res.status(400).json({ error: 'FCM token required' });
    }

    await prisma.user.update({
      where: { id: req.user.id },
      data: { fcmToken },
    });

    res.json({ message: 'FCM token updated' });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
