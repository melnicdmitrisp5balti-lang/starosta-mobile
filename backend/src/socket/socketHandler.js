const jwt = require('jsonwebtoken');
const { PrismaClient } = require('@prisma/client');
const logger = require('../utils/logger');

const { prisma } = require('../middleware/auth');

function setupSocket(io) {
  // Auth middleware for socket
  io.use(async (socket, next) => {
    try {
      const token = socket.handshake.auth.token || socket.handshake.headers.authorization?.split(' ')[1];

      if (!token) {
        return next(new Error('Authentication required'));
      }

      const decoded = jwt.verify(token, process.env.JWT_SECRET);
      const user = await prisma.user.findUnique({
        where: { id: decoded.userId },
        select: { id: true, username: true, name: true, avatarUrl: true },
      });

      if (!user) {
        return next(new Error('User not found'));
      }

      socket.user = user;
      next();
    } catch (error) {
      next(new Error('Invalid token'));
    }
  });

  io.on('connection', async (socket) => {
    const userId = socket.user.id;
    logger.debug(`User connected: ${userId}`);

    // Join user's personal room
    socket.join(`user:${userId}`);

    // Update online status
    await prisma.user.update({
      where: { id: userId },
      data: { isOnline: true },
    });

    // Join all user's chats
    const userChats = await prisma.chatMember.findMany({
      where: { userId },
      select: { chatId: true },
    });

    userChats.forEach(({ chatId }) => {
      socket.join(`chat:${chatId}`);
    });

    // Notify contacts about online status
    io.emit('user:status', { userId, isOnline: true });

    // Join a chat room
    socket.on('chat:join', async (chatId) => {
      try {
        const member = await prisma.chatMember.findUnique({
          where: { chatId_userId: { chatId, userId } },
        });
        if (member) {
          socket.join(`chat:${chatId}`);
        }
      } catch (error) {
        logger.error('Error joining chat:', error);
      }
    });

    // Leave a chat room
    socket.on('chat:leave', (chatId) => {
      socket.leave(`chat:${chatId}`);
    });

    // Typing indicator
    socket.on('typing:start', ({ chatId }) => {
      socket.to(`chat:${chatId}`).emit('typing:start', {
        chatId,
        userId,
        userName: socket.user.name,
      });
    });

    socket.on('typing:stop', ({ chatId }) => {
      socket.to(`chat:${chatId}`).emit('typing:stop', { chatId, userId });
    });

    // Mark messages as read
    socket.on('messages:read', async ({ chatId, messageIds }) => {
      try {
        if (!messageIds || !messageIds.length) return;

        await prisma.messageRead.createMany({
          data: messageIds.map(messageId => ({ messageId, userId })),
          skipDuplicates: true,
        });

        socket.to(`chat:${chatId}`).emit('message:read', {
          chatId,
          userId,
          messageIds,
        });
      } catch (error) {
        logger.error('Error marking messages as read:', error);
      }
    });

    // User status ping (keep alive)
    socket.on('ping', () => {
      socket.emit('pong');
    });

    // Disconnect
    socket.on('disconnect', async () => {
      logger.debug(`User disconnected: ${userId}`);

      try {
        await prisma.user.update({
          where: { id: userId },
          data: { isOnline: false, lastSeen: new Date() },
        });

        io.emit('user:status', { userId, isOnline: false, lastSeen: new Date() });
      } catch (error) {
        logger.error('Error updating offline status:', error);
      }
    });
  });
}

module.exports = { setupSocket };
