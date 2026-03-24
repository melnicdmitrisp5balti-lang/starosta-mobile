import { create } from 'zustand';
import { authAPI, userAPI } from '../api/api';
import { connectSocket, disconnectSocket } from '../api/socket';
import useChatStore from './chatStore';

const useAuthStore = create((set, get) => ({
  user: null,
  token: null,
  isLoading: false,
  isAuthenticated: false,

  login: async (data) => {
    set({ isLoading: true });
    try {
      const response = await authAPI.login(data);
      const { token, user } = response.data;
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(user));
      connectSocket(token);
      useChatStore.getState().setupSocketListeners();
      set({ user, token, isAuthenticated: true, isLoading: false });
      return { success: true };
    } catch (error) {
      set({ isLoading: false });
      return {
        success: false,
        error: error.response?.data?.message || 'Login failed',
      };
    }
  },

  register: async (data) => {
    set({ isLoading: true });
    try {
      const response = await authAPI.register(data);
      const { token, user } = response.data;
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(user));
      connectSocket(token);
      useChatStore.getState().setupSocketListeners();
      set({ user, token, isAuthenticated: true, isLoading: false });
      return { success: true };
    } catch (error) {
      set({ isLoading: false });
      return {
        success: false,
        error: error.response?.data?.message || 'Registration failed',
      };
    }
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    disconnectSocket();
    set({ user: null, token: null, isAuthenticated: false });
  },

  checkAuth: async () => {
    const token = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');
    if (!token) {
      set({ isAuthenticated: false });
      return;
    }
    if (savedUser) {
      set({ user: JSON.parse(savedUser), token, isAuthenticated: true });
    }
    try {
      const response = await userAPI.getMe();
      const user = response.data;
      localStorage.setItem('user', JSON.stringify(user));
      connectSocket(token);
      useChatStore.getState().setupSocketListeners();
      set({ user, token, isAuthenticated: true });
    } catch {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      set({ user: null, token: null, isAuthenticated: false });
    }
  },

  updateProfile: async (data) => {
    set({ isLoading: true });
    try {
      const response = await userAPI.updateMe(data);
      const user = response.data;
      localStorage.setItem('user', JSON.stringify(user));
      set({ user, isLoading: false });
      return { success: true };
    } catch (error) {
      set({ isLoading: false });
      return {
        success: false,
        error: error.response?.data?.message || 'Update failed',
      };
    }
  },
}));

export default useAuthStore;
