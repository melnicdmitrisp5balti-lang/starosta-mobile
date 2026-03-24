import { useEffect } from 'react';
import useChatStore from '../store/chatStore';
import useAuthStore from '../store/authStore';
import Sidebar from '../components/Common/Sidebar';
import ChatWindow from '../components/Chat/ChatWindow';
import ChatList from '../components/Chat/ChatList';

export default function Home() {
  const { activeChat, setActiveChat, loadChats } = useChatStore();
  const { isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (isAuthenticated) loadChats();
  }, [isAuthenticated, loadChats]);

  const handleBack = () => setActiveChat(null);

  return (
    <div className="flex h-full">
      {/* Desktop: always show sidebar */}
      <div className={`
        md:flex md:w-80 lg:w-96 flex-shrink-0 h-full
        ${activeChat ? 'hidden md:flex' : 'flex w-full'}
      `}>
        <div className="flex flex-col w-full h-full bg-white dark:bg-gray-800 border-r border-gray-100 dark:border-gray-700">
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100 dark:border-gray-700">
            <h1 className="font-semibold text-gray-900 dark:text-white">Chats</h1>
          </div>
          <div className="flex-1 overflow-hidden">
            <ChatList />
          </div>
        </div>
      </div>

      {/* Chat window */}
      <div className={`
        flex-1 flex flex-col h-full
        ${!activeChat ? 'hidden md:flex' : 'flex w-full'}
      `}>
        <ChatWindow onBack={handleBack} />
      </div>
    </div>
  );
}
