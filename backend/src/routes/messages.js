const express = require('express');
const router = express.Router();
const { PrismaClient } = require('@prisma/client');
const { authenticate } = require('../middleware/auth');
const { body, query, validationResult } = require('express-validator');
const { sendPushNotification } = require('../utils/notifications');

const { prisma } = require('../middleware/auth');

// Get messages in a chat
router.get('/:chatId', authenticate,
  [
    query('cursor').optional(),
    query('limit').optional().isInt({ min: 1, max: 100 }),
  ],
  async (req, res, next) => {
    try {
      const { chatId } = req.params;
      const { cursor, limit = 50 } = req.query;

      // Check membership
      const member = await prisma.chatMember.findUnique({
        where: { chatId_userId: { chatId, userId: req.user.id } },
      });

      if (!member) {
        return res.status(403).json({ error: 'Not a member of this chat' });
      }

      const messages = await prisma.message.findMany({
        where: {
          chatId,
          isDeleted: false,
          ...(cursor && {
            createdAt: { lt: new Date(cursor) },
          }),
        },
        include: {
          sender: {
            select: { id: true, username: true, name: true, avatarUrl: true },
          },
          replyTo: {
            include: {
              sender: { select: { id: true, name: true } },
            },
          },
          readBy: {
            select: { userId: true, readAt: true },
          },
        },
        orderBy: { createdAt: 'desc' },
        take: parseInt(limit),
      });

      // Mark messages as delivered
      const unreadIds = messages
        .filter(m => m.senderId !== req.user.id && !m.readBy.some(r => r.userId === req.user.id))
        .map(m => m.id);

      if (unreadIds.length > 0) {
        await prisma.messageRead.createMany({
          data: unreadIds.map(messageId => ({
            messageId,
            userId: req.user.id,
          })),
          skipDuplicates: true,
        });
      }

      res.json({
        messages: messages.reverse(),
        hasMore: messages.length === parseInt(limit),
        nextCursor: messages.length > 0 ? messages[0].createdAt.toISOString() : null,
      });
    } catch (error) {
      next(error);
    }
  }
);

// Send message
router.post('/:chatId', authenticate,
  [
    body('content').optional().trim(),
    body('fileUrl').optional(),
    body('fileName').optional(),
    body('fileType').optional(),
    body('fileSize').optional().isInt(),
    body('replyToId').optional(),
  ],
  async (req, res, next) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const { chatId } = req.params;
      const { content, fileUrl, fileName, fileType, fileSize, replyToId } = req.body;

      if (!content && !fileUrl) {
        return res.status(400).json({ error: 'Message must have content or file' });
      }

      // Check membership
      const member = await prisma.chatMember.findUnique({
        where: { chatId_userId: { chatId, userId: req.user.id } },
      });

      if (!member) {
        return res.status(403).json({ error: 'Not a member of this chat' });
      }

      const message = await prisma.message.create({
        data: {
          chatId,
          senderId: req.user.id,
          content,
          fileUrl,
          fileName,
          fileType,
          fileSize,
          replyToId,
        },
        include: {
          sender: {
            select: { id: true, username: true, name: true, avatarUrl: true },
          },
          replyTo: {
            include: {
              sender: { select: { id: true, name: true } },
            },
          },
          readBy: { select: { userId: true } },
        },
      });

      // Update chat timestamp
      await prisma.chat.update({
        where: { id: chatId },
        data: { updatedAt: new Date() },
      });

      // Emit via Socket
      const io = req.app.get('io');
      io.to(`chat:${chatId}`).emit('message:new', message);

      // Send push notifications to offline members
      const chatMembers = await prisma.chatMember.findMany({
        where: { chatId, userId: { not: req.user.id } },
        include: { user: { select: { id: true, fcmToken: true, isOnline: true, name: true } } },
      });

      for (const chatMember of chatMembers) {
        if (!chatMember.user.isOnline && chatMember.user.fcmToken) {
          await sendPushNotification(
            chatMember.user.fcmToken,
            req.user.name || 'Someone',
            content || `Sent a ${fileType || 'file'}`,
            { chatId, messageId: message.id }
          );
        }
      }

      res.status(201).json(message);
    } catch (error) {
      next(error);
    }
  }
);

// Edit message
router.patch('/:chatId/:messageId', authenticate,
  [body('content').trim().notEmpty()],
  async (req, res, next) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      const { chatId, messageId } = req.params;
      const { content } = req.body;

      const message = await prisma.message.findFirst({
        where: { id: messageId, chatId, senderId: req.user.id, isDeleted: false },
      });

      if (!message) {
        return res.status(404).json({ error: 'Message not found' });
      }

      const updated = await prisma.message.update({
        where: { id: messageId },
        data: { content, isEdited: true },
        include: {
          sender: { select: { id: true, username: true, name: true, avatarUrl: true } },
        },
      });

      const io = req.app.get('io');
      io.to(`chat:${chatId}`).emit('message:edited', updated);

      res.json(updated);
    } catch (error) {
      next(error);
    }
  }
);

// Delete message
router.delete('/:chatId/:messageId', authenticate, async (req, res, next) => {
  try {
    const { chatId, messageId } = req.params;

    const message = await prisma.message.findFirst({
      where: { id: messageId, chatId, senderId: req.user.id },
    });

    if (!message) {
      return res.status(404).json({ error: 'Message not found or unauthorized' });
    }

    const updated = await prisma.message.update({
      where: { id: messageId },
      data: { isDeleted: true, content: null },
    });

    const io = req.app.get('io');
    io.to(`chat:${chatId}`).emit('message:deleted', { messageId, chatId });

    res.json({ message: 'Message deleted' });
  } catch (error) {
    next(error);
  }
});

// Pin message
router.post('/:chatId/:messageId/pin', authenticate, async (req, res, next) => {
  try {
    const { chatId, messageId } = req.params;

    const member = await prisma.chatMember.findUnique({
      where: { chatId_userId: { chatId, userId: req.user.id } },
    });

    if (!member) {
      return res.status(403).json({ error: 'Not a member of this chat' });
    }

    const pinned = await prisma.pinnedMessage.upsert({
      where: { chatId_messageId: { chatId, messageId } },
      create: { chatId, messageId, pinnedById: req.user.id },
      update: {},
    });

    const io = req.app.get('io');
    io.to(`chat:${chatId}`).emit('message:pinned', { chatId, messageId });

    res.json(pinned);
  } catch (error) {
    next(error);
  }
});

// Unpin message
router.delete('/:chatId/:messageId/pin', authenticate, async (req, res, next) => {
  try {
    const { chatId, messageId } = req.params;

    await prisma.pinnedMessage.deleteMany({ where: { chatId, messageId } });

    const io = req.app.get('io');
    io.to(`chat:${chatId}`).emit('message:unpinned', { chatId, messageId });

    res.json({ message: 'Message unpinned' });
  } catch (error) {
    next(error);
  }
});

// Mark messages as read
router.post('/:chatId/read', authenticate, async (req, res, next) => {
  try {
    const { chatId } = req.params;
    const { messageIds } = req.body;

    if (!messageIds || !messageIds.length) {
      return res.status(400).json({ error: 'messageIds required' });
    }

    await prisma.messageRead.createMany({
      data: messageIds.map(messageId => ({
        messageId,
        userId: req.user.id,
      })),
      skipDuplicates: true,
    });

    const io = req.app.get('io');
    io.to(`chat:${chatId}`).emit('message:read', { chatId, userId: req.user.id, messageIds });

    res.json({ message: 'Messages marked as read' });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
