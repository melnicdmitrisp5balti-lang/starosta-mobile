import { useEffect, useRef } from 'react';
import useChatStore from '../../store/chatStore';
import useAuthStore from '../../store/authStore';
import Message from './Message';
import MessageInput from './MessageInput';

function Avatar({ name, avatar, online = false }) {
  const initials = name
    ? name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : '?';
  const colors = ['bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-orange-500', 'bg-pink-500', 'bg-teal-500'];
  const color = colors[(name?.charCodeAt(0) || 0) % colors.length];

  return (
    <div className="relative flex-shrink-0">
      {avatar ? (
        <img src={avatar} alt={name} className="w-9 h-9 rounded-full object-cover" />
      ) : (
        <div className={`w-9 h-9 ${color} rounded-full flex items-center justify-center text-white text-sm font-semibold`}>
          {initials}
        </div>
      )}
      {online && (
        <span className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-green-500 border-2 border-white dark:border-gray-800 rounded-full" />
      )}
    </div>
  );
}

export default function ChatWindow({ onBack }) {
  const { activeChat, messages, isLoading, typingUsers } = useChatStore();
  const { user } = useAuthStore();
  const messagesEndRef = useRef(null);

  const chatId = activeChat?._id || activeChat?.id;
  const chatMessages = messages[chatId] || [];
  const typing = typingUsers[chatId] ? Object.keys(typingUsers[chatId]) : [];

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages.length, typing.length]);

  function getChatName(chat, currentUser) {
    if (!chat) return '';
    if (chat.isGroup) return chat.name || 'Group Chat';
    const other = chat.participants?.find(
      (p) => (p._id || p.id) !== (currentUser?._id || currentUser?.id)
    );
    return other?.name || chat.name || 'Chat';
  }

  function getChatAvatar(chat, currentUser) {
    if (!chat) return null;
    if (chat.isGroup) return chat.avatar || null;
    const other = chat.participants?.find(
      (p) => (p._id || p.id) !== (currentUser?._id || currentUser?.id)
    );
    return other?.avatar || null;
  }

  function isOnline(chat, currentUser) {
    if (!chat || chat.isGroup) return false;
    const other = chat.participants?.find(
      (p) => (p._id || p.id) !== (currentUser?._id || currentUser?.id)
    );
    return other?.isOnline || false;
  }

  if (!activeChat) {
    return (
      <div className="flex-1 flex items-center justify-center bg-gray-50 dark:bg-gray-800">
        <div className="text-center text-gray-400 dark:text-gray-500">
          <svg className="w-20 h-20 mx-auto mb-4 opacity-30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <p className="font-medium text-lg">Select a chat to start messaging</p>
          <p className="text-sm mt-1">Choose from your conversations on the left</p>
        </div>
      </div>
    );
  }

  const chatName = getChatName(activeChat, user);
  const chatAvatar = getChatAvatar(activeChat, user);
  const online = isOnline(activeChat, user);

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 bg-white dark:bg-gray-800 border-b border-gray-100 dark:border-gray-700 shadow-sm">
        {onBack && (
          <button
            onClick={onBack}
            className="p-1 -ml-1 text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>
        )}
        <Avatar name={chatName} avatar={chatAvatar} online={online} />
        <div className="flex-1 min-w-0">
          <h2 className="font-semibold text-gray-900 dark:text-white text-sm truncate">{chatName}</h2>
          <p className="text-xs text-gray-500 dark:text-gray-400">
            {online ? (
              <span className="text-green-500">Online</span>
            ) : activeChat.isGroup ? (
              `${activeChat.participants?.length || 0} members`
            ) : (
              'Offline'
            )}
          </p>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto chat-messages p-4 space-y-1 bg-gray-50 dark:bg-[#1a1a2e]">
        {isLoading && chatMessages.length === 0 ? (
          <div className="flex items-center justify-center h-full">
            <svg className="animate-spin h-6 w-6 text-[#2AABEE]" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          </div>
        ) : chatMessages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400 dark:text-gray-500">
            <p className="text-sm">No messages yet. Say hello! 👋</p>
          </div>
        ) : (
          <>
            {chatMessages.map((msg) => (
              <Message
                key={msg._id || msg.id}
                message={msg}
                isOwn={(msg.sender?._id || msg.sender?.id || msg.senderId) === (user?._id || user?.id)}
                isGroup={activeChat.isGroup}
              />
            ))}
            {typing.length > 0 && (
              <div className="flex items-center gap-2 py-1">
                <div className="flex gap-1 bg-white dark:bg-gray-700 rounded-2xl px-3 py-2 shadow-sm">
                  <span className="typing-dot w-2 h-2 bg-gray-400 rounded-full inline-block" />
                  <span className="typing-dot w-2 h-2 bg-gray-400 rounded-full inline-block" />
                  <span className="typing-dot w-2 h-2 bg-gray-400 rounded-full inline-block" />
                </div>
                <span className="text-xs text-gray-400">typing...</span>
              </div>
            )}
          </>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <MessageInput chatId={chatId} />
    </div>
  );
}
