import { useState, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import useUserStore from '../../store/userStore';
import useChatStore from '../../store/chatStore';
import useAuthStore from '../../store/authStore';
import { chatAPI } from '../../api/api';

function Avatar({ name, avatar }) {
  const initials = name
    ? name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : '?';
  const colors = ['bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-orange-500', 'bg-pink-500'];
  const color = colors[(name?.charCodeAt(0) || 0) % colors.length];

  return avatar ? (
    <img src={avatar} alt={name} className="w-9 h-9 rounded-full object-cover" />
  ) : (
    <div className={`w-9 h-9 ${color} rounded-full flex items-center justify-center text-white text-sm font-semibold`}>
      {initials}
    </div>
  );
}

export default function UserSearch() {
  const [query, setQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const { searchResults, isSearching, searchUsers, clearSearch } = useUserStore();
  const { setActiveChat } = useChatStore();
  const { user: currentUser } = useAuthStore();
  const navigate = useNavigate();
  const debounceRef = useRef(null);

  const handleSearch = useCallback(
    (e) => {
      const value = e.target.value;
      setQuery(value);
      setIsOpen(true);
      clearTimeout(debounceRef.current);
      debounceRef.current = setTimeout(() => {
        searchUsers(value);
      }, 300);
    },
    [searchUsers]
  );

  const handleSelectUser = async (targetUser) => {
    setQuery('');
    setIsOpen(false);
    clearSearch();
    try {
      const response = await chatAPI.createChat({
        participantId: targetUser._id || targetUser.id,
        isGroup: false,
      });
      const chat = response.data;
      setActiveChat(chat);
      navigate('/');
    } catch {
      // silently fail
    }
  };

  const others = searchResults.filter(
    (u) => (u._id || u.id) !== (currentUser?._id || currentUser?.id)
  );

  return (
    <div className="relative px-4 py-3">
      <div className="relative">
        <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          type="text"
          value={query}
          onChange={handleSearch}
          onFocus={() => setIsOpen(true)}
          onBlur={() => setTimeout(() => setIsOpen(false), 150)}
          placeholder="Search people by name or username..."
          className="w-full pl-9 pr-3 py-2 text-sm rounded-xl bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-[#2AABEE] border-0"
        />
        {isSearching && (
          <svg className="absolute right-3 top-1/2 -translate-y-1/2 animate-spin h-4 w-4 text-[#2AABEE]" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
        )}
      </div>

      {isOpen && query && others.length > 0 && (
        <div className="absolute left-4 right-4 top-full mt-1 bg-white dark:bg-gray-800 border border-gray-100 dark:border-gray-700 rounded-xl shadow-lg z-50 overflow-hidden max-h-72 overflow-y-auto">
          {others.map((u) => (
            <button
              key={u._id || u.id}
              onMouseDown={() => handleSelectUser(u)}
              className="w-full flex items-center gap-3 px-3 py-2.5 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors text-left"
            >
              <Avatar name={u.name} avatar={u.avatar} />
              <div className="min-w-0">
                <p className="text-sm font-medium text-gray-900 dark:text-white truncate">{u.name}</p>
                {u.username && (
                  <p className="text-xs text-[#2AABEE] truncate">@{u.username}</p>
                )}
              </div>
              {u.isOnline && (
                <span className="ml-auto flex-shrink-0 w-2 h-2 bg-green-500 rounded-full" />
              )}
            </button>
          ))}
        </div>
      )}

      {isOpen && query && !isSearching && others.length === 0 && (
        <div className="absolute left-4 right-4 top-full mt-1 bg-white dark:bg-gray-800 border border-gray-100 dark:border-gray-700 rounded-xl shadow-lg z-50 px-4 py-3">
          <p className="text-sm text-gray-500 dark:text-gray-400 text-center">No users found</p>
        </div>
      )}
    </div>
  );
}
