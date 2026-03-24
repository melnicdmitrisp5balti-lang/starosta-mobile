import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import useUserStore from '../../store/userStore';
import useChatStore from '../../store/chatStore';
import useAuthStore from '../../store/authStore';
import { chatAPI } from '../../api/api';

function Avatar({ name, avatar, online = false }) {
  const initials = name
    ? name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : '?';
  const colors = ['bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-orange-500', 'bg-pink-500', 'bg-teal-500'];
  const color = colors[(name?.charCodeAt(0) || 0) % colors.length];

  return (
    <div className="relative flex-shrink-0">
      {avatar ? (
        <img src={avatar} alt={name} className="w-11 h-11 rounded-full object-cover" />
      ) : (
        <div className={`w-11 h-11 ${color} rounded-full flex items-center justify-center text-white font-semibold`}>
          {initials}
        </div>
      )}
      {online && (
        <span className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 border-2 border-white dark:border-gray-800 rounded-full" />
      )}
    </div>
  );
}

function UserCard({ user, onStartChat }) {
  return (
    <div className="flex items-center gap-3 px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
      <Avatar name={user.name} avatar={user.avatar} online={user.isOnline} />
      <div className="flex-1 min-w-0">
        <p className="font-medium text-gray-900 dark:text-white text-sm truncate">{user.name}</p>
        {user.username && (
          <p className="text-xs text-[#2AABEE] truncate">@{user.username}</p>
        )}
        {user.bio && (
          <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{user.bio}</p>
        )}
      </div>
      <button
        onClick={() => onStartChat(user)}
        className="flex-shrink-0 p-2 text-[#2AABEE] hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-full transition-colors"
        title="Start chat"
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
      </button>
    </div>
  );
}

export default function ContactsList() {
  const { contacts, loadContacts } = useUserStore();
  const { setActiveChat } = useChatStore();
  const { user: currentUser } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    loadContacts();
  }, [loadContacts]);

  const handleStartChat = async (targetUser) => {
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

  const others = contacts.filter(
    (u) => (u._id || u.id) !== (currentUser?._id || currentUser?.id)
  );

  return (
    <div className="flex-1 overflow-y-auto scrollbar-hide">
      {others.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-48 text-gray-400 dark:text-gray-500">
          <svg className="w-12 h-12 mb-3 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <p className="text-sm font-medium">No contacts found</p>
          <p className="text-xs mt-1">Search to find people</p>
        </div>
      ) : (
        others.map((u) => (
          <UserCard key={u._id || u.id} user={u} onStartChat={handleStartChat} />
        ))
      )}
    </div>
  );
}
