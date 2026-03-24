const express = require('express');
const router = express.Router();
const { PrismaClient } = require('@prisma/client');
const { authenticate } = require('../middleware/auth');
const { body, query, validationResult } = require('express-validator');

const prisma = new PrismaClient();

// Get current user profile
router.get('/me', authenticate, async (req, res, next) => {
  try {
    const user = await prisma.user.findUnique({
      where: { id: req.user.id },
      select: {
        id: true,
        email: true,
        phone: true,
        username: true,
        name: true,
        avatarUrl: true,
        status: true,
        isOnline: true,
        lastSeen: true,
        isVerified: true,
        createdAt: true,
      },
    });
    res.json(user);
  } catch (error) {
    next(error);
  }
});

// Update profile
router.patch('/me',
  authenticate,
  [
    body('name').optional().trim().notEmpty().isLength({ max: 50 }),
    body('username').optional().trim().isLength({ min: 3, max: 30 }).matches(/^[a-zA-Z0-9_]+$/),
    body('status').optional().isLength({ max: 200 }),
  ],
  async (req, res, next) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const { name, username, status } = req.body;

      if (username) {
        const existing = await prisma.user.findFirst({
          where: { username, NOT: { id: req.user.id } },
        });
        if (existing) {
          return res.status(409).json({ error: 'Username already taken' });
        }
      }

      const user = await prisma.user.update({
        where: { id: req.user.id },
        data: {
          ...(name !== undefined && { name }),
          ...(username !== undefined && { username }),
          ...(status !== undefined && { status }),
        },
        select: {
          id: true,
          email: true,
          phone: true,
          username: true,
          name: true,
          avatarUrl: true,
          status: true,
          isOnline: true,
          lastSeen: true,
        },
      });

      res.json(user);
    } catch (error) {
      next(error);
    }
  }
);

// Update avatar
router.patch('/me/avatar', authenticate, async (req, res, next) => {
  try {
    const { avatarUrl } = req.body;
    if (!avatarUrl) {
      return res.status(400).json({ error: 'Avatar URL required' });
    }

    const user = await prisma.user.update({
      where: { id: req.user.id },
      data: { avatarUrl },
      select: { id: true, avatarUrl: true },
    });

    res.json(user);
  } catch (error) {
    next(error);
  }
});

// Search users
router.get('/search', authenticate,
  [query('q').trim().notEmpty().isLength({ min: 1 })],
  async (req, res, next) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const { q } = req.query;
      const users = await prisma.user.findMany({
        where: {
          AND: [
            { NOT: { id: req.user.id } },
            {
              OR: [
                { name: { contains: q, mode: 'insensitive' } },
                { username: { contains: q, mode: 'insensitive' } },
              ],
            },
          ],
        },
        select: {
          id: true,
          username: true,
          name: true,
          avatarUrl: true,
          status: true,
          isOnline: true,
          lastSeen: true,
        },
        take: 20,
      });

      res.json(users);
    } catch (error) {
      next(error);
    }
  }
);

// Get user by ID
router.get('/:userId', authenticate, async (req, res, next) => {
  try {
    const user = await prisma.user.findUnique({
      where: { id: req.params.userId },
      select: {
        id: true,
        username: true,
        name: true,
        avatarUrl: true,
        status: true,
        isOnline: true,
        lastSeen: true,
      },
    });

    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json(user);
  } catch (error) {
    next(error);
  }
});

module.exports = router;
