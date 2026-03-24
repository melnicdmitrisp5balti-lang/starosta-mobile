require('dotenv').config();
const { server } = require('./src/app');
const { prisma } = require('./src/middleware/auth');
const logger = require('./src/utils/logger');

const PORT = process.env.PORT || 3000;

async function main() {
  try {
    await prisma.$connect();
    logger.info('Connected to database');

    server.listen(PORT, () => {
      logger.info(`Starosta Messenger server running on port ${PORT}`);
      logger.info(`Environment: ${process.env.NODE_ENV || 'development'}`);
    });
  } catch (error) {
    logger.error('Failed to start server:', error);
    process.exit(1);
  }
}

process.on('SIGTERM', async () => {
  logger.info('SIGTERM received, shutting down gracefully');
  await prisma.$disconnect();
  process.exit(0);
});

process.on('SIGINT', async () => {
  logger.info('SIGINT received, shutting down gracefully');
  await prisma.$disconnect();
  process.exit(0);
});

main();