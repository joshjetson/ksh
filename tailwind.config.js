/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './grails-app/views/**/*.gsp',
    './grails-app/views/**/*.html',
    './src/**/*.{html,js}',
  ],
  theme: {
    extend: {
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
      },
      minHeight: {
        '12': '3rem',
        '16': '4rem',
      },
      colors: {
        primary: {
          50: '#eff6ff',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
        }
      },
      fontSize: {
        'base': '16px',
      }
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
    require('@tailwindcss/typography'),
  ],
}
