package ksh

/**
 * Presentation-only tags (namespace `ksh:`). These exist so GSP partials never
 * carry computed output in scriptlets (docs/uda.md pillar 6). Add new formatting
 * tags here rather than inlining logic in a view.
 */
class KshTagLib {

    static namespace = 'ksh'

    // Event color → Tailwind classes for the calendar chip and the dot marker.
    // The literal class strings are scanned by the build (the .groovy content glob),
    // so they survive purge even though they're selected dynamically.
    static final Map EVENT_COLORS = [
        rose:    [chip: 'bg-rose-100 text-rose-700 border-rose-200',          dot: 'bg-rose-500'],
        gold:    [chip: 'bg-gold-500/15 text-gold-700 border-gold-300',       dot: 'bg-gold-500'],
        sky:     [chip: 'bg-sky-100 text-sky-700 border-sky-200',             dot: 'bg-sky-500'],
        amber:   [chip: 'bg-amber-100 text-amber-800 border-amber-200',       dot: 'bg-amber-500'],
        emerald: [chip: 'bg-emerald-100 text-emerald-700 border-emerald-200', dot: 'bg-emerald-500'],
        violet:  [chip: 'bg-violet-100 text-violet-700 border-violet-200',    dot: 'bg-violet-500'],
    ]

    private static Map colorEntry(def key) { (EVENT_COLORS[key as String] ?: EVENT_COLORS.rose) as Map }

    /** ksh:eventChip(color:'gold') → chip bg/text/border classes for a calendar event. */
    def eventChip = { attrs -> out << colorEntry(attrs.color).chip }

    /** ksh:eventDot(color:'gold') → small dot/marker bg class. */
    def eventDot = { attrs -> out << colorEntry(attrs.color).dot }

    /**
     * A CSS bar chart for an enrollment/usage trend.
     * Attr: data = List of [label: String, count: Number]. No external chart lib.
     *
     *   <ksh:trendBars data="${stats.trend}"/>
     */
    def trendBars = { attrs ->
        List data = (attrs.data ?: []) as List
        int max = (data.collect { (it.count ?: 0) as int }.max() ?: 0) as int
        out << '<div class="flex items-end gap-1.5" style="height:8rem">'
        data.each { d ->
            int count = (d.count ?: 0) as int
            int pct = max > 0 ? Math.max(3, Math.round(count * 100.0d / max) as int) : 3
            out << '<div class="flex-1 flex flex-col items-center justify-end gap-1 group">'
            out << "<span class=\"text-[10px] text-stone-400 opacity-0 group-hover:opacity-100 transition-opacity\">${count}</span>"
            out << "<div class=\"w-full bg-rose-400 group-hover:bg-rose-500 rounded-t transition-colors\" style=\"height:${pct}%\" title=\"${d.label}: ${count}\"></div>"
            out << "<span class=\"text-[10px] text-stone-400 whitespace-nowrap\">${d.label}</span>"
            out << '</div>'
        }
        out << '</div>'
    }

    /**
     * A lesson photo, or the book emoji fallback. Centralizes the image source
     * resolution (uploaded photo → thumbnail URL → emoji) so every course view
     * renders it the same way.
     *   <ksh:courseImage course="${course}" imgClass="w-full h-full object-cover" emojiClass="text-4xl"/>
     */
    def courseImage = { attrs ->
        def course = attrs.course
        String src = course?.imageSrc()
        if (src) {
            String alt = (course?.shortTitle ?: '').replaceAll('"', '&quot;')
            out << "<img src=\"${src}\" alt=\"${alt}\" class=\"${attrs.imgClass ?: 'w-full h-full object-cover'}\"/>"
        } else {
            out << "<span class=\"${attrs.emojiClass ?: 'text-4xl'}\">&#128218;</span>"
        }
    }

    /**
     * A colored pill for an enrollment / payment status.
     *   <ksh:statusBadge status="${e.status}"/>
     */
    def statusBadge = { attrs ->
        String status = (attrs.status ?: '').toString().toUpperCase()
        Map colors = [
            REQUESTED: 'bg-amber-100 text-amber-800',
            CONFIRMED: 'bg-sky-100 text-sky-800',
            PAID     : 'bg-emerald-100 text-emerald-800',
            ACTIVE   : 'bg-sky-100 text-sky-800',
            COMPLETED: 'bg-stone-200 text-stone-700',
            CANCELLED: 'bg-rose-100 text-rose-700',
            UNPAID   : 'bg-stone-100 text-stone-600',
            DEPOSIT  : 'bg-amber-100 text-amber-800'
        ]
        String cls = colors[status] ?: 'bg-stone-100 text-stone-600'
        String label = status ? status.toLowerCase().capitalize() : '—'
        out << "<span class=\"inline-block text-xs font-medium px-2 py-0.5 rounded-full ${cls}\">${label}</span>"
    }

    /**
     * A colored pill for a security role.
     *   <ksh:roleBadge role="ROLE_ADMIN"/>
     */
    def roleBadge = { attrs ->
        String role = (attrs.role ?: '').toString()
        Map map = [
            ROLE_ADMIN    : ['Admin',            'bg-rose-100 text-rose-700'],
            ROLE_TEACHER  : ['Teacher',          'bg-sky-100 text-sky-800'],
            ROLE_MODERATOR: ['Community Leader', 'bg-gold-500/15 text-gold-700'],
            ROLE_USER     : ['Student',          'bg-stone-100 text-stone-600']
        ]
        def entry = map[role] ?: [role, 'bg-stone-100 text-stone-600']
        out << "<span class=\"inline-block text-xs font-medium px-2 py-0.5 rounded-full ${entry[1]}\">${entry[0]}</span>"
    }

    /**
     * Render a K-credit amount compactly: 0 → "Free", whole numbers drop the decimal.
     *   <ksh:credits amount="${course.costKCredits}"/>
     */
    def credits = { attrs ->
        def raw = attrs.amount
        BigDecimal amount = (raw == null ? 0 : raw) as BigDecimal
        if (amount == 0) {
            out << 'Free'
            return
        }
        String n = (amount.stripTrailingZeros().scale() <= 0) ? amount.toBigInteger().toString() : amount.toPlainString()
        out << "${n} ₩cr"
    }
}
