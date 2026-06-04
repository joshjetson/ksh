package ksh

/**
 * Presentation-only tags (namespace `ksh:`). These exist so GSP partials never
 * carry computed output in scriptlets (docs/uda.md pillar 6). Add new formatting
 * tags here rather than inlining logic in a view.
 */
class KshTagLib {

    static namespace = 'ksh'

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
            ROLE_ADMIN  : ['Admin',   'bg-rose-100 text-rose-700'],
            ROLE_TEACHER: ['Teacher', 'bg-sky-100 text-sky-800'],
            ROLE_USER   : ['Student', 'bg-stone-100 text-stone-600']
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
