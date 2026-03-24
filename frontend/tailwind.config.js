/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        telegram: {
          blue: '#2AABEE',
          dark: '#212121',
          darkSecondary: '#2b2b2b',
          messageSent: '#2AABEE',
          messageReceived: '#ffffff',
        },
      },
    },
  },
  plugins: [],
}

