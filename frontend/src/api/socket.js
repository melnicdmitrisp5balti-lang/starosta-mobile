import { io } from 'socket.io-client';

const SOCKET_URL = import.meta.env.VITE_API_URL
  ? import.meta.env.VITE_API_URL.replace('/api', '')
  : 'http://localhost:3000';

let socket = null;

export const connectSocket = (token) => {
  if (socket && socket.connected) {
    socket.disconnect();
  }

  socket = io(SOCKET_URL, {
    autoConnect: true,
    transports: ['websocket', 'polling'],
    auth: {
      token: token,
    },
    reconnection: true,
    reconnectionDelay: 1000,
    reconnectionDelayMax: 5000,
    reconnectionAttempts: 5,
  });

  socket.on('connect', () => {
    console.log('✅ Socket connected:', socket.id);
  });

  socket.on('disconnect', () => {
    console.log('❌ Socket disconnected');
  });

  socket.on('error', (error) => {
    console.error('🔴 Socket error:', error);
  });

  return socket;
};

export const disconnectSocket = () => {
  if (socket && socket.connected) {
    socket.disconnect();
  }
  socket = null;
};

export const getSocket = () => socket;

export const joinRoom = (chatId) => {
  if (socket && socket.connected) {
    socket.emit('chat:join', chatId);
  }
};

export const leaveRoom = (chatId) => {
  if (socket && socket.connected) {
    socket.emit('chat:leave', chatId);
  }
};