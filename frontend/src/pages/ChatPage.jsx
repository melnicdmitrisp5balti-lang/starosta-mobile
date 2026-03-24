import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import useChatStore from '../store/chatStore';
import { chatAPI } from '../api/api';
import ChatWindow from '../components/Chat/ChatWindow';

export default function ChatPage() {
  const { id } = useParams();
  const { setActiveChat, activeChat } = useChatStore();

  useEffect(() => {
    const load = async () => {
      try {
        const response = await chatAPI.getChat(id);
        setActiveChat(response.data);
      } catch {
        // silently fail
      }
    };
    if (id && (activeChat?._id || activeChat?.id) !== id) {
      load();
    }
  }, [id, setActiveChat, activeChat]);

  return (
    <div className="flex flex-col h-full">
      <ChatWindow />
    </div>
  );
}
