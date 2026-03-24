let admin = null;

try {
  const firebaseAdmin = require('firebase-admin');

  if (process.env.FIREBASE_PROJECT_ID && process.env.FIREBASE_PRIVATE_KEY) {
    admin = firebaseAdmin.initializeApp({
      credential: firebaseAdmin.credential.cert({
        projectId: process.env.FIREBASE_PROJECT_ID,
        privateKeyId: process.env.FIREBASE_PRIVATE_KEY_ID,
        privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
        clientId: process.env.FIREBASE_CLIENT_ID,
      }),
    });
  }
} catch (error) {
  // Firebase not configured
}

async function sendPushNotification(fcmToken, title, body, data = {}) {
  if (!admin || !fcmToken) return;

  try {
    await admin.messaging().send({
      token: fcmToken,
      notification: { title, body },
      data: Object.fromEntries(
        Object.entries(data).map(([k, v]) => [k, String(v)])
      ),
      android: {
        priority: 'high',
        notification: {
          channelId: 'messages',
          sound: 'default',
          clickAction: 'FLUTTER_NOTIFICATION_CLICK',
        },
      },
    });
  } catch (error) {
    // Log but don't throw - notifications are non-critical
    const logger = require('./logger');
    logger.warn('Push notification failed:', error.message);
  }
}

module.exports = { sendPushNotification };
