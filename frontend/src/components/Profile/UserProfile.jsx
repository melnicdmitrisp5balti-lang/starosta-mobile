import useAuthStore from '../../store/authStore';

function Avatar({ name, avatar, size = 'xl' }) {
  const sizes = { sm: 'w-10 h-10 text-base', md: 'w-16 h-16 text-2xl', xl: 'w-24 h-24 text-4xl' };
  const initials = name
    ? name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : '?';
  const colors = ['bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-orange-500', 'bg-pink-500', 'bg-teal-500'];
  const color = colors[(name?.charCodeAt(0) || 0) % colors.length];

  return avatar ? (
    <img src={avatar} alt={name} className={`${sizes[size]} rounded-full object-cover`} />
  ) : (
    <div className={`${sizes[size]} ${color} rounded-full flex items-center justify-center text-white font-bold`}>
      {initials}
    </div>
  );
}

export default function UserProfile({ targetUser, onMessage, onEdit }) {
  const { user: currentUser } = useAuthStore();
  const profileUser = targetUser || currentUser;
  const isSelf = (profileUser?._id || profileUser?.id) === (currentUser?._id || currentUser?.id);

  if (!profileUser) return null;

  return (
    <div className="flex flex-col items-center p-6 space-y-4">
      <div className="relative">
        <Avatar name={profileUser.name} avatar={profileUser.avatar} size="xl" />
        {profileUser.isOnline && (
          <span className="absolute bottom-1 right-1 w-5 h-5 bg-green-500 border-3 border-white dark:border-gray-800 rounded-full" />
        )}
      </div>

      <div className="text-center">
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white">{profileUser.name}</h2>
        {profileUser.username && (
          <p className="text-sm text-[#2AABEE] mt-0.5">@{profileUser.username}</p>
        )}
        {profileUser.bio && (
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-2 max-w-xs text-center">{profileUser.bio}</p>
        )}
        <p className="text-xs mt-2">
          {profileUser.isOnline ? (
            <span className="text-green-500 font-medium">● Online</span>
          ) : (
            <span className="text-gray-400">Offline</span>
          )}
        </p>
      </div>

      <div className="flex gap-3 w-full max-w-xs">
        {!isSelf && onMessage && (
          <button
            onClick={onMessage}
            className="flex-1 py-2.5 bg-[#2AABEE] hover:bg-[#229ED9] text-white text-sm font-medium rounded-xl transition-colors flex items-center justify-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            Message
          </button>
        )}
        {isSelf && onEdit && (
          <button
            onClick={onEdit}
            className="flex-1 py-2.5 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-200 text-sm font-medium rounded-xl transition-colors flex items-center justify-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            Edit Profile
          </button>
        )}
      </div>

      {profileUser.email && isSelf && (
        <div className="w-full max-w-xs bg-gray-50 dark:bg-gray-700/50 rounded-xl p-4 space-y-2">
          <div className="flex items-center gap-2 text-sm">
            <svg className="w-4 h-4 text-[#2AABEE]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
            <span className="text-gray-600 dark:text-gray-300">{profileUser.email}</span>
          </div>
        </div>
      )}
    </div>
  );
}
