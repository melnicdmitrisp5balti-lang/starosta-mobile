import { useState, useMemo } from 'react';
import { formatDistanceToNow } from 'date-fns';
import useChatStore from '../../store/chatStore';
import useAuthStore from '../../store/authStore';

function Avatar({ name, avatar, size = 'md', online = false }) {
  const sizeClasses = { sm: 'w-9 h-9 text-sm', md: 'w-12 h-12 text-base', lg: 'w-14 h-14 text-lg' };
  const initials = name
    ? name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : '?';
  const colors = ['bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-orange-500', 'bg-pink-500', 'bg-teal-500'];
  const color = colors[(name?.charCodeAt(0) || 0) % colors.length];

  return (
    <div className="relative flex-shrink-0">
      {avatar ? (
        <img src={avatar} alt={name} className={`${sizeClasses[size]} rounded-full object-cover`} />
      ) : (
        <div className={`${sizeClasses[size]} ${color} rounded-full flex items-center justify-center text-white font-semibold`}>
          {initials}
        </div>
      )}
      {online && (
        <span className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 border-2 border-white dark:border-gray-800 rounded-full" />
      )}
    </div>
  );
}

export default function ChatList() {
  const [search, setSearch] = useState('');
  const { chats, activeChat, setActiveChat, isLoading } = useChatStore();
  const { user } = useAuthStore();

  const filtered = useMemo(() => {
    if (!search.trim()) return chats;
    const q = search.toLowerCase();
    return chats.filter((c) => {
      const name = getChatName(c, user);
      return name.toLowerCase().includes(q);
    });
  }, [chats, search, user]);

  function getChatName(chat, currentUser) {
    if (chat.isGroup) return chat.name || 'Group Chat';
    const other = chat.participants?.find(
      (p) => (p._id || p.id) !== (currentUser?._id || currentUser?.id)
    );
    return other?.name || chat.name || 'Chat';
  }

  function getChatAvatar(chat, currentUser) {
    if (chat.isGroup) return chat.avatar || null;
    const other = chat.participants?.find(
      (p) => (p._id || p.id) !== (currentUser?._id || currentUser?.id)
    );
    return other?.avatar || null;
  }

  function isOnline(chat, currentUser) {
    if (chat.isGroup) return false;
    const other = chat.participants?.find(
      (p) => (p._id || p.id) !== (currentUser?._id || currentUser?.id)
    );
    return other?.isOnline || chat.onlineParticipants?.includes(other?._id || other?.id) || false;
  }

  function getLastMessagePreview(chat) {
    const msg = chat.lastMessage;
    if (!msg) return 'No messages yet';
    if (msg.type === 'image') return '📷 Photo';
    if (msg.type === 'file') return '📎 File';
    return msg.content || '';
  }

  function formatTime(date) {
    if (!date) return '';
    try {
      return formatDistanceToNow(new Date(date), { addSuffix: false });
    } catch {
      return '';
    }
  }

  const sortedChats = useMemo(() => {
    return [...filtered].sort((a, b) => {
      const aTime = a.lastMessage?.createdAt || a.updatedAt || a.createdAt || 0;
      const bTime = b.lastMessage?.createdAt || b.updatedAt || b.createdAt || 0;
      return new Date(bTime) - new Date(aTime);
    });
  }, [filtered]);

  return (
    <div className="flex flex-col h-full">
      <div className="p-3 border-b border-gray-100 dark:border-gray-700">
        <div className="relative">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search chats..."
            className="w-full pl-9 pr-3 py-2 text-sm rounded-xl bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-[#2AABEE] border-0"
          />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto scrollbar-hide">
        {isLoading && chats.length === 0 ? (
          <div className="flex items-center justify-center h-32">
            <svg className="animate-spin h-6 w-6 text-[#2AABEE]" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          </div>
        ) : sortedChats.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-48 text-gray-400 dark:text-gray-500">
            <svg className="w-12 h-12 mb-3 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <p className="text-sm font-medium">No chats yet</p>
            <p className="text-xs mt-1">Start a conversation from Contacts</p>
          </div>
        ) : (
          sortedChats.map((chat) => {
            const chatId = chat._id || chat.id;
            const name = getChatName(chat, user);
            const avatar = getChatAvatar(chat, user);
            const online = isOnline(chat, user);
            const isActive = (activeChat?._id || activeChat?.id) === chatId;

            return (
              <button
                key={chatId}
                onClick={() => setActiveChat(chat)}
                className={`w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors text-left ${
                  isActive ? 'bg-blue-50 dark:bg-blue-900/20' : ''
                }`}
              >
                <Avatar name={name} avatar={avatar} online={online} />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-0.5">
                    <span className="font-medium text-gray-900 dark:text-white truncate text-sm">
                      {name}
                    </span>
                    <span className="text-xs text-gray-400 dark:text-gray-500 flex-shrink-0 ml-2">
                      {formatTime(chat.lastMessage?.createdAt || chat.updatedAt)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
                      {getLastMessagePreview(chat)}
                    </p>
                    {chat.unreadCount > 0 && (
                      <span className="ml-2 flex-shrink-0 bg-[#2AABEE] text-white text-xs font-bold rounded-full min-w-[20px] h-5 flex items-center justify-center px-1">
                        {chat.unreadCount > 99 ? '99+' : chat.unreadCount}
                      </span>
                    )}
                  </div>
                </div>
              </button>
            );
          })
        )}
      </div>
    </div>
  );
}
