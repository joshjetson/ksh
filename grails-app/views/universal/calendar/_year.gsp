<%-- Year calendar — 12 mini-month grids. Model: cal (ScheduleService.yearView → year,
     months, prevYear, nextYear), user. Tap a month name → month view; tap a day → day view.
     Days with events are tinted; today is filled. --%>
<g:if test="${cal}">
<%
    String todayIso = new Date().format('yyyy-MM-dd')
    def yearVals  = { y -> '{"template": "calendar/year", "data[cal]": "service:scheduleService:yearView", "data[user]": "currentUser", "year": "' + y + '"}' }
    def monthVals = { m -> '{"template": "calendar/month", "data[cal]": "service:scheduleService:monthView", "data[user]": "currentUser", "year": "' + cal.year + '", "month": "' + m + '"}' }
    def dayVals   = { iso -> '{"template": "calendar/day", "data[day]": "service:scheduleService:dayView", "data[user]": "currentUser", "iso": "' + iso + '"}' }
%>
<div>
    <div class="flex justify-center mb-4">
        <g:render template="/universal/calendar/calToggle" model="[active: 'year', year: cal.year, month: 1, iso: todayIso]"/>
    </div>

    <div class="flex items-center justify-between mb-4">
        <h2 class="text-xl font-bold text-stone-800">${cal.year}</h2>
        <div class="flex items-center gap-1">
            <button class="h-10 w-10 rounded-full hover:bg-cream-100 text-stone-600 text-lg" aria-label="Previous year"
                    hx-get="/universal/showView" hx-vals='${yearVals(cal.prevYear)}' hx-target="#content" hx-swap="innerHTML">&lsaquo;</button>
            <button class="h-10 w-10 rounded-full hover:bg-cream-100 text-stone-600 text-lg" aria-label="Next year"
                    hx-get="/universal/showView" hx-vals='${yearVals(cal.nextYear)}' hx-target="#content" hx-swap="innerHTML">&rsaquo;</button>
        </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <g:each in="${cal.months}" var="mo">
            <div class="card p-3">
                <button type="button" class="font-semibold text-rose-700 hover:underline text-sm mb-2"
                        hx-get="/universal/showView" hx-vals='${monthVals(mo.monthNum)}' hx-target="#content" hx-swap="innerHTML">${mo.label}</button>
                <div class="grid grid-cols-7 gap-0.5 text-center">
                    <g:each in="${['S', 'M', 'T', 'W', 'T', 'F', 'S']}" var="dow"><div class="text-[9px] text-stone-300 font-semibold">${dow}</div></g:each>
                    <g:each in="${mo.weeks}" var="week"><g:if test="${week.any { it.inMonth }}"><g:each in="${week}" var="cell">
                        <g:if test="${cell.inMonth}">
                            <button type="button"
                                    class="aspect-square flex items-center justify-center rounded text-[10px] min-h-0 ${cell.today ? 'bg-rose-600 text-white font-bold' : (cell.hasEvents ? 'bg-rose-50 text-rose-700 font-semibold' : 'text-stone-600 hover:bg-cream-100')}"
                                    hx-get="/universal/showView" hx-vals='${dayVals(cell.iso)}' hx-target="#content" hx-swap="innerHTML">${cell.day}</button>
                        </g:if>
                        <g:else><div class="aspect-square"></div></g:else>
                    </g:each></g:if></g:each>
                </div>
            </div>
        </g:each>
    </div>
</div>
</g:if>
<g:else>
    <p class="text-sm text-stone-400 py-10 text-center">Calendar unavailable.</p>
</g:else>
