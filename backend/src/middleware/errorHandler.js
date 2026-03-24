const logger = require('../utils/logger');

const errorHandler = (err, req, res, next) => {
  logger.error('Error:', {
    message: err.message,
    stack: err.stack,
    path: req.path,
    method: req.method,
  });

  if (err.name === 'ValidationError' || err.type === 'validation') {
    return res.status(400).json({
      error: 'Validation error',
      details: err.message,
    });
  }

  if (err.name === 'UnauthorizedError' || err.status === 401) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  if (err.code === 'P2002') {
    return res.status(409).json({
      error: 'Conflict',
      details: 'A record with this value already exists',
    });
  }

  if (err.code === 'P2025') {
    return res.status(404).json({ error: 'Resource not found' });
  }

  const statusCode = err.statusCode || err.status || 500;
  const message = process.env.NODE_ENV === 'production'
    ? 'Internal server error'
    : err.message;

  res.status(statusCode).json({ error: message });
};

module.exports = errorHandler;
