import UserSearch from '../components/Contacts/UserSearch';
import ContactsList from '../components/Contacts/ContactsList';

export default function ContactsPage() {
  return (
    <div className="flex flex-col h-full bg-white dark:bg-gray-800">
      <div className="flex items-center px-4 py-3 border-b border-gray-100 dark:border-gray-700">
        <h1 className="font-semibold text-gray-900 dark:text-white">Contacts</h1>
      </div>
      <UserSearch />
      <div className="flex-1 overflow-hidden">
        <ContactsList />
      </div>
    </div>
  );
}
