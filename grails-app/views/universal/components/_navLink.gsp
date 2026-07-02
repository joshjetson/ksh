<%-- One navigation entry, reused across the desktop sidebar, the mobile bottom tab
     bar, and the mobile "More" sheet. Model:
       key     — stable id (kept in sync across all renders of this destination)
       label   — text
       icon    — icon name (see map below)
       vals    — the showView hx-vals JSON string (template + data instructions)
       url     — plain endpoint instead of showView (e.g. an admin controller action)
       variant — 'side' | 'tab' | 'drawer'
       active  — true to render selected on first paint --%>
<%
    def base = [
        side:   'nav-side flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-stone-600 hover:bg-cream-100 w-full text-left',
        tab:    'nav-tab flex-1 flex flex-col items-center justify-center gap-0.5 py-1.5 text-[11px] font-medium text-stone-500 min-h-[44px]',
        drawer: 'nav-drawer flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium text-stone-700 hover:bg-cream-100 w-full text-left',
    ]
    def icons = [
        home:      'M3 12l9-9 9 9M5 10v10a1 1 0 001 1h4v-6h4v6h4a1 1 0 001-1V10',
        calendar:  'M8 7V3m8 4V3M3 11h18M5 7h14a2 2 0 012 2v10a2 2 0 01-2 2H5a2 2 0 01-2-2V9a2 2 0 012-2z',
        doc:       'M9 12h6m-6 4h6m2 6H7a2 2 0 01-2-2V6a2 2 0 012-2h7l5 5v11a2 2 0 01-2 2z',
        photo:     'M3 6h18v12H3zM3 16l4-4a3 3 0 014 0l3 3m-1-1l2-2a3 3 0 014 0l2 2',
        heart:     'M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z',
        bullhorn:  'M11 5L6 9H2v6h4l5 4V5zM15.54 8.46a5 5 0 010 7.07',
        grid:      'M4 5h6v6H4zM14 5h6v6h-6zM4 15h6v6H4zM14 15h6v6h-6z',
        users:     'M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87M12 12a4 4 0 100-8 4 4 0 000 8z',
        clipboard: 'M9 5h6m-3 0V3M7 5h10a2 2 0 012 2v12a2 2 0 01-2 2H7a2 2 0 01-2-2V7a2 2 0 012-2z',
        chat:      'M8 10h.01M12 10h.01M16 10h.01M21 12a8 8 0 01-11.3 7.3L3 21l1.7-6.7A8 8 0 1121 12z',
        account:   'M12 12a4 4 0 100-8 4 4 0 000 8zM4.5 20a7.5 7.5 0 0115 0',
        cog:       'M12 15a3 3 0 100-6 3 3 0 000 6zM19.4 13a1 1 0 00.2 1.1l.1.1a2 2 0 01-2.8 2.8l-.1-.1a1 1 0 00-1.1-.2 1 1 0 00-.6.9V20a2 2 0 01-4 0v-.1a1 1 0 00-.7-.9 1 1 0 00-1.1.2l-.1.1a2 2 0 01-2.8-2.8l.1-.1a1 1 0 00.2-1.1 1 1 0 00-.9-.6H4a2 2 0 010-4h.1a1 1 0 00.9-.7 1 1 0 00-.2-1.1l-.1-.1a2 2 0 012.8-2.8l.1.1a1 1 0 001.1.2H8a1 1 0 00.6-.9V4a2 2 0 014 0v.1a1 1 0 00.7.9 1 1 0 001.1-.2l.1-.1a2 2 0 012.8 2.8l-.1.1a1 1 0 00-.2 1.1V8a1 1 0 00.9.6H20a2 2 0 010 4h-.1a1 1 0 00-.9.7z',
        search:    'M21 21l-4.3-4.3M11 18a7 7 0 100-14 7 7 0 000 14z',
        bookmark:  'M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-4-7 4V5z',
        book:      'M12 6.5c-1.5-1.3-3.5-2-5.5-2-1 0-2 .2-3 .5v13c1-.3 2-.5 3-.5 2 0 4 .7 5.5 2 1.5-1.3 3.5-2 5.5-2 1 0 2 .2 3 .5v-13c-1-.3-2-.5-3-.5-2 0-4 .7-5.5 2zM12 6.5v13',
        plus:      'M12 5v14M5 12h14',
        tag:       'M7 7h.01M3 5a2 2 0 012-2h5.6a2 2 0 011.4.6l8.4 8.4a2 2 0 010 2.8L15 20.2a2 2 0 01-2.8 0L3.6 11.8A2 2 0 013 10.4V5z',
        inbox:     'M3 13h4l2 3h6l2-3h4M5 6h14a2 2 0 012 2v10a2 2 0 01-2 2H5a2 2 0 01-2-2V8a2 2 0 012-2z',
    ]
    def cls = base[variant ?: 'side'] + (active ? ' nav-active' : '')
    def iconPath = icons[icon]
    boolean tab = (variant == 'tab')
    // Most links load a UDA view via showView+hx-vals; pass `url` for a plain endpoint
    // (e.g. an admin controller action) instead.
    String hxGet = url ?: '/universal/showView'
%>
<button type="button" data-nav="${key}" class="${cls}"
        hx-get="${hxGet}"<g:if test="${!url}"> hx-vals='${vals}'</g:if> hx-target="#content" hx-swap="innerHTML"
        onclick="kshNav(this)">
    <g:if test="${iconPath}">
        <svg class="${tab ? 'h-6 w-6' : 'h-5 w-5'} shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="${iconPath}"/>
        </svg>
    </g:if>
    <span>${label}</span>
</button>
