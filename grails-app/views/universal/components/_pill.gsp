<%-- Small pill badge (status / category chips like "Pinned", "Staff only", "Inactive").
     Named _pill because KSH's _badge.gsp is the achievement-badge display. For
     enrollment/payment status use ksh:statusBadge; for roles use ksh:roleBadge. Model:
       label — text
       tone  — cream (default, neutral) | gold | rose | stone | emerald
       size  — 'sm' (default) | 'xs' --%>
<%
    def tones = [
        cream:   'bg-cream-200 text-stone-500',
        gold:    'bg-gold-500/15 text-gold-700',
        rose:    'bg-rose-100 text-rose-600',
        stone:   'bg-stone-100 text-stone-500',
        emerald: 'bg-emerald-100 text-emerald-700',
    ]
    def sizes = [xs: 'text-[10px] px-1.5 py-0.5', sm: 'text-xs px-2 py-0.5']
    def cls = (tones[tone] ?: tones.cream) + ' ' + (sizes[size] ?: sizes.sm)
%>
<span class="inline-block font-semibold rounded-full ${cls}">${label}</span>
