import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000/api';

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  googleLogin: (token) => api.post('/auth/google', { token }),
};

export const userAPI = {
  getMe: () => api.get('/users/me'),
  updateMe: (data) => api.patch('/users/me', data),
  searchUsers: (q) => api.get(`/users/search?q=${encodeURIComponent(q)}`),
  getUser: (id) => api.get(`/users/${id}`),
  uploadAvatar: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

export const chatAPI = {
  getChats: () => api.get('/chats'),
  createChat: (data) => api.post('/chats', data),
  getChat: (id) => api.get(`/chats/${id}`),
  updateChat: (id, data) => api.patch(`/chats/${id}`, data),
  deleteChat: (id) => api.delete(`/chats/${id}`),
};

export const messageAPI = {
  getMessages: (chatId, cursor) =>
    api.get(`/messages/${chatId}`, cursor ? { params: { cursor } } : {}),
  sendMessage: (chatId, data) => api.post(`/messages/${chatId}`, data),
  editMessage: (id, data) => api.patch(`/messages/${id}`, data),
  deleteMessage: (id) => api.delete(`/messages/${id}`),
};

export const fileAPI = {
  upload: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

export default api;
