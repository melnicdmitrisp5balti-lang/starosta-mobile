import { useState } from 'react';
import useAuthStore from '../store/authStore';
import UserProfile from '../components/Profile/UserProfile';
import EditProfile from '../components/Profile/EditProfile';

export default function ProfilePage() {
  const [isEditing, setIsEditing] = useState(false);
  const { user, logout } = useAuthStore();

  return (
    <div className="flex flex-col h-full bg-white dark:bg-gray-800">
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100 dark:border-gray-700">
        <h1 className="font-semibold text-gray-900 dark:text-white">Profile</h1>
        <button
          onClick={logout}
          className="text-sm text-red-500 hover:text-red-600 font-medium"
        >
          Sign out
        </button>
      </div>

      <div className="flex-1 overflow-y-auto">
        {isEditing ? (
          <EditProfile onClose={() => setIsEditing(false)} />
        ) : (
          <UserProfile
            targetUser={user}
            onEdit={() => setIsEditing(true)}
          />
        )}
      </div>
    </div>
  );
}
