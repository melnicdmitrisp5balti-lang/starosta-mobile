const express = require('express');
const router = express.Router();
const { PrismaClient } = require('@prisma/client');
const { authenticate } = require('../middleware/auth');
const { body, validationResult } = require('express-validator');
const { sendPushNotification } = require('../utils/notifications');

const prisma = new PrismaClient();

// Get user's chats
router.get('/', authenticate, async (req, res, next) => {
  try {
    const chats = await prisma.chat.findMany({
      where: {
        members: { some: { userId: req.user.id } },
      },
      include: {
        members: {
          include: {
            user: {
              select: {
                id: true, username: true, name: true,
                avatarUrl: true, isOnline: true, lastSeen: true,
              },
            },
          },
        },
        messages: {
          orderBy: { createdAt: 'desc' },
          take: 1,
          include: {
            sender: { select: { id: true, name: true } },
          },
        },
        pinnedMessages: {
          include: {
            message: {
              include: {
                sender: { select: { id: true, name: true } },
              },
            },
          },
          take: 1,
          orderBy: { createdAt: 'desc' },
        },
        _count: {
          select: {
            messages: {
              where: {
                isDeleted: false,
                readBy: { none: { userId: req.user.id } },
                NOT: { senderId: req.user.id },
              },
            },
          },
        },
      },
      orderBy: { updatedAt: 'desc' },
    });

    res.json(chats);
  } catch (error) {
    next(error);
  }
});

// Get single chat
router.get('/:chatId', authenticate, async (req, res, next) => {
  try {
    const chat = await prisma.chat.findFirst({
      where: {
        id: req.params.chatId,
        members: { some: { userId: req.user.id } },
      },
      include: {
        members: {
          include: {
            user: {
              select: {
                id: true, username: true, name: true,
                avatarUrl: true, isOnline: true, lastSeen: true,
              },
            },
          },
        },
        pinnedMessages: {
          include: {
            message: {
              include: {
                sender: { select: { id: true, name: true } },
              },
            },
          },
          orderBy: { createdAt: 'desc' },
        },
      },
    });

    if (!chat) {
      return res.status(404).json({ error: 'Chat not found' });
    }

    res.json(chat);
  } catch (error) {
    next(error);
  }
});

// Create private chat
router.post('/private',
  authenticate,
  [body('userId').notEmpty()],
  async (req, res, next) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const { userId } = req.body;

      if (userId === req.user.id) {
        return res.status(400).json({ error: 'Cannot create chat with yourself' });
      }

      const targetUser = await prisma.user.findUnique({ where: { id: userId } });
      if (!targetUser) {
        return res.status(404).json({ error: 'User not found' });
      }

      // Check if private chat already exists
      const existing = await prisma.chat.findFirst({
        where: {
          type: 'PRIVATE',
          AND: [
            { members: { some: { userId: req.user.id } } },
            { members: { some: { userId } } },
          ],
        },
        include: {
          members: {
            include: {
              user: {
                select: {
                  id: true, username: true, name: true,
                  avatarUrl: true, isOnline: true, lastSeen: true,
                },
              },
            },
          },
        },
      });

      if (existing) {
        return res.json(existing);
      }

      const chat = await prisma.chat.create({
        data: {
          type: 'PRIVATE',
          createdById: req.user.id,
          members: {
            create: [
              { userId: req.user.id, isAdmin: true },
              { userId },
            ],
          },
        },
        include: {
          members: {
            include: {
              user: {
                select: {
                  id: true, username: true, name: true,
                  avatarUrl: true, isOnline: true, lastSeen: true,
                },
              },
            },
          },
        },
      });

      const io = req.app.get('io');
      io.to(`user:${userId}`).emit('chat:new', chat);

      res.status(201).json(chat);
    } catch (error) {
      next(error);
    }
  }
);

// Create group chat
router.post('/group',
  authenticate,
  [
    body('name').trim().notEmpty().isLength({ max: 100 }),
    body('memberIds').isArray({ min: 1 }),
  ],
  async (req, res, next) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const { name, memberIds } = req.body;

      const uniqueIds = [...new Set([...memberIds, req.user.id])];

      const chat = await prisma.chat.create({
        data: {
          type: 'GROUP',
          name,
          createdById: req.user.id,
          members: {
            create: uniqueIds.map(uid => ({
              userId: uid,
              isAdmin: uid === req.user.id,
            })),
          },
        },
        include: {
          members: {
            include: {
              user: {
                select: {
                  id: true, username: true, name: true,
                  avatarUrl: true, isOnline: true, lastSeen: true,
                },
              },
            },
          },
        },
      });

      const io = req.app.get('io');
      memberIds.forEach(uid => {
        if (uid !== req.user.id) {
          io.to(`user:${uid}`).emit('chat:new', chat);
        }
      });

      res.status(201).json(chat);
    } catch (error) {
      next(error);
    }
  }
);

// Update group chat
router.patch('/:chatId',
  authenticate,
  [
    body('name').optional().trim().notEmpty().isLength({ max: 100 }),
    body('avatarUrl').optional(),
  ],
  async (req, res, next) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const { chatId } = req.params;

      const member = await prisma.chatMember.findUnique({
        where: { chatId_userId: { chatId, userId: req.user.id } },
      });

      if (!member || !member.isAdmin) {
        return res.status(403).json({ error: 'Only admins can update the group' });
      }

      const { name, avatarUrl } = req.body;
      const chat = await prisma.chat.update({
        where: { id: chatId },
        data: {
          ...(name !== undefined && { name }),
          ...(avatarUrl !== undefined && { avatarUrl }),
        },
        include: {
          members: {
            include: {
              user: { select: { id: true, username: true, name: true, avatarUrl: true } },
            },
          },
        },
      });

      const io = req.app.get('io');
      io.to(`chat:${chatId}`).emit('chat:updated', chat);

      res.json(chat);
    } catch (error) {
      next(error);
    }
  }
);

// Add member to group
router.post('/:chatId/members', authenticate,
  [body('userId').notEmpty()],
  async (req, res, next) => {
    try {
      const { chatId } = req.params;
      const { userId } = req.body;

      const member = await prisma.chatMember.findUnique({
        where: { chatId_userId: { chatId, userId: req.user.id } },
      });

      if (!member || !member.isAdmin) {
        return res.status(403).json({ error: 'Only admins can add members' });
      }

      const existing = await prisma.chatMember.findUnique({
        where: { chatId_userId: { chatId, userId } },
      });

      if (existing) {
        return res.status(409).json({ error: 'User already in chat' });
      }

      await prisma.chatMember.create({ data: { chatId, userId } });

      const chat = await prisma.chat.findUnique({
        where: { id: chatId },
        include: {
          members: {
            include: {
              user: { select: { id: true, username: true, name: true, avatarUrl: true } },
            },
          },
        },
      });

      const io = req.app.get('io');
      io.to(`chat:${chatId}`).emit('chat:memberAdded', { chatId, userId });
      io.to(`user:${userId}`).emit('chat:new', chat);

      res.json({ message: 'Member added' });
    } catch (error) {
      next(error);
    }
  }
);

// Remove member from group
router.delete('/:chatId/members/:userId', authenticate, async (req, res, next) => {
  try {
    const { chatId, userId } = req.params;

    const requester = await prisma.chatMember.findUnique({
      where: { chatId_userId: { chatId, userId: req.user.id } },
    });

    if (!requester) {
      return res.status(403).json({ error: 'Not a member of this chat' });
    }

    if (!requester.isAdmin && userId !== req.user.id) {
      return res.status(403).json({ error: 'Only admins can remove members' });
    }

    await prisma.chatMember.delete({
      where: { chatId_userId: { chatId, userId } },
    });

    const io = req.app.get('io');
    io.to(`chat:${chatId}`).emit('chat:memberRemoved', { chatId, userId });

    res.json({ message: 'Member removed' });
  } catch (error) {
    next(error);
  }
});

// Delete chat (leave group or delete private)
router.delete('/:chatId', authenticate, async (req, res, next) => {
  try {
    const { chatId } = req.params;

    await prisma.chatMember.delete({
      where: { chatId_userId: { chatId, userId: req.user.id } },
    });

    // If no members left, delete the chat
    const count = await prisma.chatMember.count({ where: { chatId } });
    if (count === 0) {
      await prisma.chat.delete({ where: { id: chatId } });
    }

    const io = req.app.get('io');
    io.to(`chat:${chatId}`).emit('chat:memberLeft', { chatId, userId: req.user.id });

    res.json({ message: 'Left chat successfully' });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
