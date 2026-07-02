<%-- Reusable avatar. If `src` is given (KSH users can have real avatar URLs — preset
     SVGs or an earned avatar badge image), renders the image; otherwise initials in a
     colored circle, color derived deterministically from the name (same person → same
     color everywhere). Model:
       src      (String)  — optional image URL (user.avatar)
       name     (String)  — display name (or username)
       size     (String)  — Tailwind size classes, default 'h-9 w-9'
       textSize (String)  — Tailwind text size, default 'text-sm'
     Used by user management, message stamps, and the channel member strip. --%>
<%
    String nm = (name ?: '?').toString().trim()
    def parts = nm ? nm.split(/\s+/).findAll { it } : []
    String initials = parts ? parts.take(2).collect { it[0] }.join().toUpperCase() : '?'
    def palette = [
        'bg-rose-100 text-rose-700', 'bg-gold-500/20 text-gold-700', 'bg-emerald-100 text-emerald-700',
        'bg-sky-100 text-sky-700', 'bg-amber-100 text-amber-800', 'bg-violet-100 text-violet-700',
    ]
    String cls = palette[(nm.hashCode() & 0x7fffffff) % palette.size()]
%>
<g:if test="${src}">
    <img src="${src}" alt="${nm}" class="${size ?: 'h-9 w-9'} rounded-full object-cover shrink-0"/>
</g:if>
<g:else>
    <span class="${size ?: 'h-9 w-9'} rounded-full ${cls} font-bold flex items-center justify-center shrink-0 ${textSize ?: 'text-sm'}">${initials}</span>
</g:else>
