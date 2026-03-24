import { create } from 'zustand';
import { chatAPI, messageAPI } from '../api/api';
import { socket, joinRoom, leaveRoom } from '../api/socket';

const useChatStore = create((set, get) => ({
  chats: [],
  activeChat: null,
  messages: {},
  isLoading: false,
  typingUsers: {},

  loadChats: async () => {
    set({ isLoading: true });
    try {
      const response = await chatAPI.getChats();
      set({ chats: response.data, isLoading: false });
    } catch {
      set({ isLoading: false });
    }
  },

  setActiveChat: (chat) => {
    const { activeChat } = get();
    if (activeChat) leaveRoom(activeChat._id || activeChat.id);
    if (chat) joinRoom(chat._id || chat.id);
    set({ activeChat: chat });
    if (chat) get().loadMessages(chat._id || chat.id);
  },

  loadMessages: async (chatId) => {
    set({ isLoading: true });
    try {
      const response = await messageAPI.getMessages(chatId);
      const msgs = Array.isArray(response.data)
        ? response.data
        : response.data.messages || [];
      set((state) => ({
        messages: { ...state.messages, [chatId]: msgs },
        isLoading: false,
      }));
    } catch {
      set({ isLoading: false });
    }
  },

  sendMessage: async (chatId, content, type = 'text') => {
    try {
      const payload = { content, type };
      socket.emit('send_message', { chatId, ...payload });
      const response = await messageAPI.sendMessage(chatId, payload);
      const msg = response.data;
      set((state) => ({
        messages: {
          ...state.messages,
          [chatId]: [...(state.messages[chatId] || []), msg],
        },
        chats: state.chats.map((c) =>
          (c._id || c.id) === chatId
            ? { ...c, lastMessage: msg, updatedAt: msg.createdAt }
            : c
        ),
      }));
      return { success: true, message: msg };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Send failed',
      };
    }
  },

  editMessage: async (messageId, data) => {
    try {
      const response = await messageAPI.editMessage(messageId, data);
      const updated = response.data;
      set((state) => {
        const newMessages = { ...state.messages };
        for (const chatId in newMessages) {
          newMessages[chatId] = newMessages[chatId].map((m) =>
            (m._id || m.id) === messageId ? updated : m
          );
        }
        return { messages: newMessages };
      });
      return { success: true };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Edit failed',
      };
    }
  },

  deleteMessage: async (messageId) => {
    try {
      await messageAPI.deleteMessage(messageId);
      set((state) => {
        const newMessages = { ...state.messages };
        for (const chatId in newMessages) {
          newMessages[chatId] = newMessages[chatId].filter(
            (m) => (m._id || m.id) !== messageId
          );
        }
        return { messages: newMessages };
      });
      return { success: true };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Delete failed',
      };
    }
  },

  addMessage: (msg) => {
    const chatId = msg.chatId || msg.chat;
    if (!chatId) return;
    set((state) => {
      const existing = state.messages[chatId] || [];
      const alreadyExists = existing.some(
        (m) => (m._id || m.id) === (msg._id || msg.id)
      );
      if (alreadyExists) return state;
      return {
        messages: { ...state.messages, [chatId]: [...existing, msg] },
        chats: state.chats.map((c) =>
          (c._id || c.id) === chatId
            ? { ...c, lastMessage: msg, updatedAt: msg.createdAt }
            : c
        ),
      };
    });
  },

  updateMessage: (msg) => {
    const chatId = msg.chatId || msg.chat;
    if (!chatId) return;
    set((state) => ({
      messages: {
        ...state.messages,
        [chatId]: (state.messages[chatId] || []).map((m) =>
          (m._id || m.id) === (msg._id || msg.id) ? msg : m
        ),
      },
    }));
  },

  setTyping: (chatId, userId, isTyping) => {
    set((state) => {
      const chatTyping = state.typingUsers[chatId] || {};
      if (isTyping) {
        return { typingUsers: { ...state.typingUsers, [chatId]: { ...chatTyping, [userId]: true } } };
      } else {
        const updated = { ...chatTyping };
        delete updated[userId];
        return { typingUsers: { ...state.typingUsers, [chatId]: updated } };
      }
    });
  },

  setupSocketListeners: () => {
    socket.on('new_message', (msg) => {
      get().addMessage(msg);
    });

    socket.on('message_updated', (msg) => {
      get().updateMessage(msg);
    });

    socket.on('message_deleted', ({ messageId, chatId }) => {
      set((state) => ({
        messages: {
          ...state.messages,
          [chatId]: (state.messages[chatId] || []).filter(
            (m) => (m._id || m.id) !== messageId
          ),
        },
      }));
    });

    socket.on('typing_start', ({ chatId, userId }) => {
      get().setTyping(chatId, userId, true);
    });

    socket.on('typing_stop', ({ chatId, userId }) => {
      get().setTyping(chatId, userId, false);
    });

    socket.on('user_online', ({ userId }) => {
      set((state) => ({
        chats: state.chats.map((c) =>
          c.participants?.some((p) => (p._id || p.id) === userId)
            ? { ...c, onlineParticipants: [...(c.onlineParticipants || []), userId] }
            : c
        ),
      }));
    });

    socket.on('user_offline', ({ userId }) => {
      set((state) => ({
        chats: state.chats.map((c) => ({
          ...c,
          onlineParticipants: (c.onlineParticipants || []).filter((id) => id !== userId),
        })),
      }));
    });
  },
}));

export default useChatStore;
