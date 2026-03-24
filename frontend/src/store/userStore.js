import { create } from 'zustand';
import { userAPI } from '../api/api';

const useUserStore = create((set) => ({
  searchResults: [],
  contacts: [],
  isSearching: false,

  searchUsers: async (q) => {
    if (!q.trim()) {
      set({ searchResults: [] });
      return;
    }
    set({ isSearching: true });
    try {
      const response = await userAPI.searchUsers(q);
      const results = Array.isArray(response.data)
        ? response.data
        : response.data.users || [];
      set({ searchResults: results, isSearching: false });
    } catch {
      set({ searchResults: [], isSearching: false });
    }
  },

  loadContacts: async () => {
    try {
      const response = await userAPI.searchUsers('');
      const results = Array.isArray(response.data)
        ? response.data
        : response.data.users || [];
      set({ contacts: results });
    } catch {
      set({ contacts: [] });
    }
  },

  clearSearch: () => set({ searchResults: [] }),
}));

export default useUserStore;
