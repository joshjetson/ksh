<%-- Month calendar. Model: cal (ScheduleService.monthView), user. Tap a day → day view
     (calendar/day). Events render as color chips; blackout (closure) days render muted
     with the reason; staff also see an enrollment-count badge (cal.staff — the service
     only populates enrollments for staff). All roles view; staff/admin manage events and
     blackouts from the day view. --%>
<g:if test="${cal}">
<%
    def navVals = { y, m -> '{"template": "calendar/month", "data[cal]": "service:scheduleService:monthView", "data[user]": "currentUser", "year": "' + y + '", "month": "' + m + '"}' }
    def dayVals = { iso -> '{"template": "calendar/day", "data[day]": "service:scheduleService:dayView", "data[user]": "currentUser", "iso": "' + iso + '"}' }
    String todayIso = new Date().format('yyyy-MM-dd')
%>
<div>
    <div class="flex justify-center mb-4">
        <g:render template="/universal/calendar/calToggle" model="[active: 'month', year: cal.year, month: cal.month, iso: todayIso]"/>
    </div>

    <div class="flex items-center justify-between mb-4">
        <h2 class="text-xl font-bold text-stone-800">${cal.monthLabel}</h2>
        <div class="flex items-center gap-1">
            <button class="h-10 w-10 rounded-full hover:bg-cream-100 text-stone-600 text-lg" aria-label="Previous month"
                    hx-get="/universal/showView" hx-vals='${navVals(cal.prev.year, cal.prev.month)}'
                    hx-target="#content" hx-swap="innerHTML">‹</button>
            <button class="px-3 h-10 rounded-full hover:bg-cream-100 text-sm font-semibold text-rose-700"
                    hx-get="/universal/showView" hx-vals='{"template": "calendar/month", "data[cal]": "service:scheduleService:monthView", "data[user]": "currentUser"}'
                    hx-target="#content" hx-swap="innerHTML">Today</button>
            <button class="h-10 w-10 rounded-full hover:bg-cream-100 text-stone-600 text-lg" aria-label="Next month"
                    hx-get="/universal/showView" hx-vals='${navVals(cal.next.year, cal.next.month)}'
                    hx-target="#content" hx-swap="innerHTML">›</button>
        </div>
    </div>

    <div class="grid grid-cols-7 gap-px mb-1">
        <g:each in="${['S','M','T','W','T','F','S']}" var="dow">
            <div class="text-center text-xs font-semibold text-stone-400 py-1">${dow}</div>
        </g:each>
    </div>

    <div class="grid grid-cols-7 gap-1">
        <g:each in="${cal.weeks}" var="week">
            <g:each in="${week}" var="cell">
                <button type="button"
                        class="text-left min-h-[4.5rem] sm:min-h-[6rem] rounded-xl p-1.5 border transition-colors ${cell.today ? 'border-rose-400 bg-rose-50' : (cell.blackout ? 'border-stone-200 bg-stone-100 hover:bg-stone-200/60' : 'border-cream-200 bg-white hover:bg-cream-50')} ${cell.inMonth ? '' : 'opacity-40'}"
                        hx-get="/universal/showView" hx-vals='${dayVals(cell.iso)}'
                        hx-target="#content" hx-swap="innerHTML">
                    <span class="inline-flex items-center justify-center h-6 w-6 rounded-full text-xs font-semibold ${cell.today ? 'bg-rose-600 text-white' : 'text-stone-600'}">${cell.day}</span>
                    <div class="mt-1 space-y-0.5">
                        <g:if test="${cell.blackout}">
                            <div class="text-[10px] leading-tight rounded-full px-1.5 py-0.5 bg-stone-200 text-stone-500 font-semibold truncate">${cell.blackout.reason ?: 'Closed'}</div>
                        </g:if>
                        <g:each in="${cell.events.take(2)}" var="ev">
                            <div class="text-[10px] leading-tight rounded px-1 py-0.5 border truncate ${ksh.eventChip(color: ev.color)}">${ev.allDay ? '' : (ev.time ? ev.time.replace(':00', '') + ' ' : '')}${ev.title}</div>
                        </g:each>
                        <g:if test="${cell.events.size() > 2}">
                            <div class="text-[10px] text-stone-400 font-medium px-1">+${cell.events.size() - 2} more</div>
                        </g:if>
                        <g:if test="${cal.staff && cell.enrollCount > 0}">
                            <div class="text-[10px] text-rose-600 font-semibold px-1">${cell.enrollCount} enroll${cell.enrollCount > 1 ? 's' : ''}</div>
                        </g:if>
                    </div>
                </button>
            </g:each>
        </g:each>
    </div>
</div>
</g:if>
<g:else>
    <p class="text-sm text-stone-400 py-10 text-center">Calendar unavailable.</p>
</g:else>
