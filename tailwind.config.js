/** @type {import('tailwindcss').Config} */
module.exports = {
  // Scan EVERY place a class name can appear — views, taglibs, controllers/services
  // that render markup, and JS that sets classNames. Missing a path = purged styles.
  content: [
    './grails-app/**/*.{gsp,html,groovy,js}',
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
        // Cream — KSH's warm bisque identity. 200 is the classic #ffe4c4 body
        // background the app has always had; the rest of the scale radiates from it.
        cream: {
          50:  '#fff9f2',
          100: '#fff1e0',
          200: '#ffe4c4',
          300: '#f2cfa4',
          400: '#dfb583',
        },
        // Gold — warm accent / call-to-action, harmonizes with bisque + rose.
        gold: {
          400: '#e8b04b',
          500: '#d99a2b',
          600: '#b97e1e',
          700: '#94631a',
        },
        // (rose — the KSH primary — and stone neutrals come from Tailwind defaults.)
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
