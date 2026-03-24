import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import ChatList from '../Chat/ChatList';
import useChatStore from '../../store/chatStore';
import { chatAPI } from '../../api/api';
import useAuthStore from '../../store/authStore';

export default function Sidebar() {
  const { loadChats } = useChatStore();
  const { isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (isAuthenticated) loadChats();
  }, [loadChats, isAuthenticated]);

  const handleNewChat = async () => {
    // Navigate to contacts to start a new chat
    window.location.href = '/contacts';
  };

  return (
    <div className="flex flex-col h-full bg-white dark:bg-gray-800 border-r border-gray-100 dark:border-gray-700">
      {/* Sidebar Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800">
        <h1 className="font-semibold text-gray-900 dark:text-white text-base">Starosta</h1>
        <div className="flex items-center gap-1">
          <Link
            to="/contacts"
            className="p-2 text-gray-400 hover:text-[#2AABEE] hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-full transition-colors"
            title="New chat"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
          </Link>
        </div>
      </div>

      {/* Chat List */}
      <div className="flex-1 overflow-hidden">
        <ChatList />
      </div>
    </div>
  );
}
