# Starosta Messenger 📱

Полностью функциональное мобильное приложение-мессенджер для Android в стиле Telegram.

## 🎨 Дизайн

Приложение выполнено в стиле [starostahub.me](https://starostahub.me/):
- Тёмная тема с cyan/teal акцентами (#00D4E8)
- Анимированные градиентные орбы на экранах аутентификации
- Material Design 3

## ✨ Функциональность (все 15 требований)

1. Регистрация/вход — Email, Google Sign-In, Twilio SMS
2. Профиль пользователя — имя, аватар, статус
3. Список чатов с последним сообщением и временем
4. Личные чаты между пользователями
5. Сообщения в реальном времени (WebSocket)
6. Отправка фото, видео и файлов
7. Групповые чаты (добавление/удаление участников)
8. Поиск пользователей по имени/username
9. Push-уведомления (Firebase Cloud Messaging)
10. Статус сообщений (отправлено/доставлено/прочитано)
11. Онлайн/оффлайн статус
12. Редактирование и удаление сообщений
13. Закрепление сообщений
14. Тёмная и светлая тема
15. JWT + bcrypt безопасность

## 🏗️ Стек

**Android:** Kotlin · Jetpack Compose · MVVM · Hilt · Room · Retrofit · Socket.io · FCM  
**Backend:** Node.js · Express · Socket.io · PostgreSQL · Prisma · JWT · Twilio · Firebase Admin

## 🚀 Быстрый старт

### Backend
```bash
cd backend
npm install
cp .env.example .env   # заполнить переменные
npx prisma generate
npx prisma db push
npm run dev
```

### Android
1. Открыть папку `android` в Android Studio
2. Добавить `google-services.json` в `android/app/`
3. Обновить `BASE_URL` в `app/build.gradle.kts`
4. Запустить (▶️)

## 📁 Структура

```
starosta_mobile/
├── backend/          # Node.js + Express + Socket.io
│   ├── src/routes/   # auth, users, chats, messages, files
│   ├── src/socket/   # Socket.io handler
│   ├── src/prisma/   # PostgreSQL schema
│   └── server.js
└── android/          # Kotlin + Jetpack Compose
    └── app/src/main/kotlin/com/starosta/messenger/
        ├── ui/screens/     # 11 экранов
        ├── viewmodel/      # AuthVM, ChatVM, MessageVM, UserVM
        ├── repository/     # Auth, Chat, Message, User repos
        ├── data/           # Room DB + Retrofit + WebSocket
        └── di/             # Hilt modules
```

---

# starosta_mobile (original)