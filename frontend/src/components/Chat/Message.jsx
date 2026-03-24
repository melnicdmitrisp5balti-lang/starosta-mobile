import { useState, useRef, useCallback } from 'react';
import { format } from 'date-fns';
import useChatStore from '../../store/chatStore';

function ContextMenu({ x, y, onEdit, onDelete, onClose, isOwn }) {
  return (
    <>
      <div className="fixed inset-0 z-40" onClick={onClose} />
      <div
        className="fixed z-50 bg-white dark:bg-gray-700 rounded-xl shadow-lg border border-gray-100 dark:border-gray-600 overflow-hidden min-w-[140px]"
        style={{ top: y, left: x }}
      >
        {isOwn && (
          <button
            onClick={onEdit}
            className="w-full flex items-center gap-2 px-4 py-2.5 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            Edit
          </button>
        )}
        {isOwn && (
          <button
            onClick={onDelete}
            className="w-full flex items-center gap-2 px-4 py-2.5 text-sm text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            Delete
          </button>
        )}
      </div>
    </>
  );
}

export default function Message({ message, isOwn, isGroup }) {
  const [contextMenu, setContextMenu] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState('');
  const { editMessage, deleteMessage } = useChatStore();
  const longPressTimer = useRef(null);

  const handleContextMenu = useCallback((e) => {
    if (!isOwn) return;
    e.preventDefault();
    const x = Math.min(e.clientX, window.innerWidth - 160);
    const y = Math.min(e.clientY, window.innerHeight - 120);
    setContextMenu({ x, y });
  }, [isOwn]);

  const handleTouchStart = useCallback(() => {
    if (!isOwn) return;
    longPressTimer.current = setTimeout(() => {
      setContextMenu({ x: 60, y: 200 });
    }, 600);
  }, [isOwn]);

  const handleTouchEnd = useCallback(() => {
    if (longPressTimer.current) clearTimeout(longPressTimer.current);
  }, []);

  const handleEdit = () => {
    setEditContent(message.content || '');
    setIsEditing(true);
    setContextMenu(null);
  };

  const handleDelete = async () => {
    setContextMenu(null);
    await deleteMessage(message._id || message.id);
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    if (!editContent.trim()) return;
    await editMessage(message._id || message.id, { content: editContent });
    setIsEditing(false);
  };

  const formatTime = (date) => {
    if (!date) return '';
    try {
      return format(new Date(date), 'HH:mm');
    } catch {
      return '';
    }
  };

  const renderContent = () => {
    if (message.type === 'image' || (message.fileUrl && /\.(jpg|jpeg|png|gif|webp)$/i.test(message.fileUrl))) {
      return (
        <div className="max-w-[240px]">
          <img
            src={message.fileUrl || message.content}
            alt="Image"
            className="rounded-xl w-full object-cover"
            loading="lazy"
          />
        </div>
      );
    }
    if (message.type === 'file' || message.fileUrl) {
      return (
        <a
          href={message.fileUrl || message.content}
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-2 text-sm underline"
        >
          <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" />
          </svg>
          {message.fileName || 'Download file'}
        </a>
      );
    }
    return <span className="text-sm leading-relaxed whitespace-pre-wrap break-words">{message.content}</span>;
  };

  return (
    <>
      <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'} mb-1 message-enter`}>
        <div
          className={`relative max-w-[75%] group`}
          onContextMenu={handleContextMenu}
          onTouchStart={handleTouchStart}
          onTouchEnd={handleTouchEnd}
        >
          {isGroup && !isOwn && message.sender?.name && (
            <p className="text-xs font-medium text-[#2AABEE] mb-1 px-1">
              {message.sender.name}
            </p>
          )}

          <div
            className={`px-3 py-2 rounded-2xl shadow-sm ${
              isOwn
                ? 'bg-[#2AABEE] text-white rounded-br-sm'
                : 'bg-white dark:bg-gray-700 text-gray-900 dark:text-white rounded-bl-sm'
            }`}
          >
            {isEditing ? (
              <form onSubmit={handleEditSubmit} className="flex items-center gap-2">
                <input
                  value={editContent}
                  onChange={(e) => setEditContent(e.target.value)}
                  className="bg-transparent border-b border-current text-sm focus:outline-none w-full"
                  autoFocus
                />
                <button type="submit" className="text-xs opacity-80 hover:opacity-100">Save</button>
                <button type="button" onClick={() => setIsEditing(false)} className="text-xs opacity-60 hover:opacity-100">✕</button>
              </form>
            ) : (
              renderContent()
            )}

            <div className={`flex items-center justify-end gap-1 mt-0.5 ${isOwn ? 'text-blue-100' : 'text-gray-400'}`}>
              {message.isEdited && <span className="text-xs opacity-70">edited</span>}
              <span className="text-xs opacity-80">{formatTime(message.createdAt)}</span>
              {isOwn && (
                <span className="text-xs">
                  {message.readBy?.length > 1 ? (
                    <svg className="w-3.5 h-3.5 inline" viewBox="0 0 16 11" fill="currentColor">
                      <path d="M11.071.653a.75.75 0 00-1.06 1.06L5.5 6.224 3.24 3.963a.75.75 0 00-1.06 1.06l2.79 2.791a.75.75 0 001.06 0l5.041-5.1zm3.18 0a.75.75 0 00-1.06 1.06L8.68 6.224 7.5 5.043l-1.06 1.06 1.71 1.71a.75.75 0 001.06 0l5.041-5.1z" />
                    </svg>
                  ) : (
                    <svg className="w-3 h-3 inline" viewBox="0 0 12 8" fill="currentColor">
                      <path d="M11.071.653a.75.75 0 00-1.06 1.06L5.5 6.224 1.74 2.463a.75.75 0 00-1.06 1.06l4.29 4.291a.75.75 0 001.06 0L11.07.653z" />
                    </svg>
                  )}
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {contextMenu && (
        <ContextMenu
          x={contextMenu.x}
          y={contextMenu.y}
          isOwn={isOwn}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onClose={() => setContextMenu(null)}
        />
      )}
    </>
  );
}
