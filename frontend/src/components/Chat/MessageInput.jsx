import { useState, useRef, useCallback, useEffect } from 'react';
import useChatStore from '../../store/chatStore';
import { getSocket } from '../../api/socket';
import { fileAPI } from '../../api/api';

const EMOJIS = ['😀', '😂', '❤️', '👍', '👎', '🎉', '🔥', '😮', '😢', '🤔', '👋', '✅'];
const TYPING_TIMEOUT_MS = 1500;

export default function MessageInput({ chatId }) {
  const [content, setContent] = useState('');
  const [showEmoji, setShowEmoji] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const { sendMessage } = useChatStore();
  const textareaRef = useRef(null);
  const fileInputRef = useRef(null);
  const typingTimeoutRef = useRef(null);
  const isTypingRef = useRef(false);

  const emitTypingStop = useCallback(() => {
    const socket = getSocket();
    if (socket && isTypingRef.current) {
      socket.emit('typing_stop', { chatId });
      isTypingRef.current = false;
    }
  }, [chatId]);

  const handleChange = (e) => {
    setContent(e.target.value);
    const el = textareaRef.current;
    if (el) {
      el.style.height = 'auto';
      el.style.height = Math.min(el.scrollHeight, 120) + 'px';
    }

    const socket = getSocket();
    if (!isTypingRef.current && socket) {
      socket.emit('typing_start', { chatId });
      isTypingRef.current = true;
    }
    clearTimeout(typingTimeoutRef.current);
    typingTimeoutRef.current = setTimeout(emitTypingStop, TYPING_TIMEOUT_MS);
  };

  const handleSend = async () => {
    const trimmed = content.trim();
    if (!trimmed || !chatId) return;
    setContent('');
    if (textareaRef.current) textareaRef.current.style.height = 'auto';
    emitTypingStop();
    clearTimeout(typingTimeoutRef.current);
    await sendMessage(chatId, trimmed, 'text');
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setIsUploading(true);
    try {
      const response = await fileAPI.upload(file);
      const fileUrl = response.data.url || response.data.fileUrl;
      const isImage = file.type.startsWith('image/');
      await sendMessage(chatId, fileUrl, isImage ? 'image' : 'file');
    } catch {
      // silently fail
    } finally {
      setIsUploading(false);
      e.target.value = '';
    }
  };

  const insertEmoji = (emoji) => {
    setContent((prev) => prev + emoji);
    setShowEmoji(false);
    textareaRef.current?.focus();
  };

  useEffect(() => {
    return () => {
      clearTimeout(typingTimeoutRef.current);
      emitTypingStop();
    };
  }, [emitTypingStop]);

  return (
    <div className="px-3 py-2 bg-white dark:bg-gray-800 border-t border-gray-100 dark:border-gray-700">
      {showEmoji && (
        <div className="bg-white dark:bg-gray-700 border border-gray-100 dark:border-gray-600 rounded-xl p-2 mb-2 shadow-lg">
          <div className="flex flex-wrap gap-1">
            {EMOJIS.map((emoji) => (
              <button
                key={emoji}
                onClick={() => insertEmoji(emoji)}
                className="text-xl hover:scale-125 transition-transform p-1"
              >
                {emoji}
              </button>
            ))}
          </div>
        </div>
      )}

      <div className="flex items-end gap-2">
        <button
          onClick={() => setShowEmoji(!showEmoji)}
          className="flex-shrink-0 p-2 text-gray-400 hover:text-[#2AABEE] transition-colors"
          title="Emoji"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </button>

        <div className="flex-1 relative">
          <textarea
            ref={textareaRef}
            value={content}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            placeholder="Write a message..."
            rows={1}
            className="w-full resize-none py-2 px-3 text-sm rounded-2xl bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-[#2AABEE] border-0 max-h-[120px] leading-relaxed"
          />
        </div>

        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          onChange={handleFileChange}
          accept="image/*,application/pdf,.doc,.docx,.txt"
        />

        {content.trim() ? (
          <button
            onClick={handleSend}
            className="flex-shrink-0 w-9 h-9 bg-[#2AABEE] hover:bg-[#229ED9] text-white rounded-full flex items-center justify-center transition-colors shadow-sm"
          >
            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
              <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
            </svg>
          </button>
        ) : (
          <button
            onClick={() => fileInputRef.current?.click()}
            disabled={isUploading}
            className="flex-shrink-0 p-2 text-gray-400 hover:text-[#2AABEE] transition-colors disabled:opacity-50"
            title="Attach file"
          >
            {isUploading ? (
              <svg className="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
            ) : (
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" />
              </svg>
            )}
          </button>
        )}
      </div>
    </div>
  );
}