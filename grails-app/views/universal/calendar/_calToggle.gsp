<%-- Calendar view switch (Day / Month / Year). Model: active ('day'|'month'|'year'),
     year, month, iso. Day defaults to `iso` (today when coming from Month/Year). --%>
<%
    def monthVals = '{"template": "calendar/month", "data[cal]": "service:scheduleService:monthView", "data[user]": "currentUser", "year": "' + year + '", "month": "' + month + '"}'
    def yearVals  = '{"template": "calendar/year", "data[cal]": "service:scheduleService:yearView", "data[user]": "currentUser", "year": "' + year + '"}'
    def dayVals   = '{"template": "calendar/day", "data[day]": "service:scheduleService:dayView", "data[user]": "currentUser", "iso": "' + (iso ?: '') + '"}'
    String base = 'px-4 py-1.5 rounded-full transition-colors min-h-0'
    String act  = 'bg-white text-rose-700 shadow-sm'
    String inact = 'text-stone-500 hover:text-stone-700'
%>
<div class="inline-flex rounded-full bg-cream-100 p-1 text-sm font-semibold">
    <button type="button" class="${base} ${active == 'day' ? act : inact}" hx-get="/universal/showView" hx-vals='${dayVals}' hx-target="#content" hx-swap="innerHTML">Day</button>
    <button type="button" class="${base} ${active == 'month' ? act : inact}" hx-get="/universal/showView" hx-vals='${monthVals}' hx-target="#content" hx-swap="innerHTML">Month</button>
    <button type="button" class="${base} ${active == 'year' ? act : inact}" hx-get="/universal/showView" hx-vals='${yearVals}' hx-target="#content" hx-swap="innerHTML">Year</button>
</div>
