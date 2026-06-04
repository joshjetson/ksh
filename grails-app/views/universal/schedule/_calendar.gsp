<div class="flex items-center justify-between mb-4 gap-3">
    <h2 class="text-xl font-bold text-stone-800">Calendar</h2>
    <div class="flex items-center gap-2">
        <button hx-get="/universal/showView"
                hx-vals='{"template": "schedule/calendar", "data[cal]": "service:scheduleService:monthView", "year": "${cal.prev.year}", "month": "${cal.prev.month}"}'
                hx-target="#content" hx-swap="innerHTML"
                class="px-3 py-2 rounded-lg hover:bg-stone-100 text-stone-600 min-h-[44px]" aria-label="Previous month">‹</button>
        <span class="text-sm font-medium text-stone-700 w-40 text-center">${cal.monthLabel}</span>
        <button hx-get="/universal/showView"
                hx-vals='{"template": "schedule/calendar", "data[cal]": "service:scheduleService:monthView", "year": "${cal.next.year}", "month": "${cal.next.month}"}'
                hx-target="#content" hx-swap="innerHTML"
                class="px-3 py-2 rounded-lg hover:bg-stone-100 text-stone-600 min-h-[44px]" aria-label="Next month">›</button>
    </div>
</div>

<!-- Block a date -->
<form hx-post="/universal/save?domainName=BlackoutDate"
      hx-vals='{"template": "schedule/calendar", "data[cal]": "service:scheduleService:monthView", "year": "${cal.year}", "month": "${cal.month}"}'
      hx-target="#content" hx-swap="innerHTML"
      class="bg-white rounded-xl shadow-sm border border-stone-200 p-4 mb-4 flex flex-wrap items-end gap-3">
    <div>
        <label class="block text-xs text-stone-500 mb-1">Block a date</label>
        <input type="date" name="blackoutDate" required
               class="px-3 py-2 border border-stone-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-rose-500"/>
    </div>
    <div class="flex-1 min-w-[160px]">
        <label class="block text-xs text-stone-500 mb-1">Reason</label>
        <input type="text" name="reason" placeholder="Holiday, closure…"
               class="block w-full px-3 py-2 border border-stone-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-rose-500"/>
    </div>
    <button type="submit" class="px-4 py-2 bg-rose-700 hover:bg-rose-800 text-white rounded-lg text-sm font-medium min-h-[44px]">Block date</button>
</form>

<!-- Month grid -->
<div class="bg-white rounded-xl shadow-sm border border-stone-200 p-3 overflow-x-auto">
    <div class="min-w-[42rem]">
    <div class="grid grid-cols-7 gap-1 mb-1">
        <g:each in="${['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']}" var="dn">
            <div class="text-center text-xs text-stone-400 py-1">${dn}</div>
        </g:each>
    </div>
    <g:each in="${cal.weeks}" var="week">
        <div class="grid grid-cols-7 gap-1 mb-1">
            <g:each in="${week}" var="d">
                <div class="rounded-lg p-1.5 min-h-[64px] text-xs border ${d.blackout ? 'bg-rose-50 border-rose-200' : (d.inMonth ? 'bg-white border-stone-100' : 'bg-stone-50 border-stone-100')} ${d.today ? 'ring-2 ring-rose-400' : ''}">
                    <div class="flex items-center justify-between">
                        <span class="${d.inMonth ? 'text-stone-700 font-medium' : 'text-stone-300'}">${d.day}</span>
                        <g:if test="${d.blackout}">
                            <button hx-post="/universal/delete/${d.blackout.id}?domainName=BlackoutDate"
                                    hx-vals='{"template": "schedule/calendar", "data[cal]": "service:scheduleService:monthView", "year": "${cal.year}", "month": "${cal.month}"}'
                                    hx-target="#content" hx-swap="innerHTML"
                                    hx-confirm="Remove this blackout?"
                                    class="text-rose-400 hover:text-rose-600 leading-none" title="${d.blackout.reason ?: 'Closed'}">&times;</button>
                        </g:if>
                    </div>
                    <g:if test="${d.blackout}">
                        <div class="text-[10px] text-rose-500 truncate mt-0.5">${d.blackout.reason ?: 'Closed'}</div>
                    </g:if>
                    <g:if test="${d.enrollCount > 0}">
                        <div class="mt-1 text-[10px] text-rose-600 font-medium">${d.enrollCount} enroll${d.enrollCount > 1 ? 's' : ''}</div>
                    </g:if>
                </div>
            </g:each>
        </div>
    </g:each>
    </div>
</div>
