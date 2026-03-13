<%
    def variants = [
        primary: 'bg-rose-700 hover:bg-rose-800 text-white focus:ring-rose-500',
        secondary: 'bg-stone-200 hover:bg-stone-300 text-stone-800 focus:ring-stone-400',
        danger: 'bg-red-600 hover:bg-red-700 text-white focus:ring-red-500'
    ]
    def cls = variants[variant ?: 'primary']
%>
<button type="${type ?: 'button'}"
        class="px-4 py-3 rounded-lg font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 transition-colors text-base ${cls} ${full ? 'w-full' : ''}">${text}</button>
