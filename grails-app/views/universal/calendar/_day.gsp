<%-- Single-day view. Model: day (ScheduleService.dayView), user. Lists the day's events;
     staff/admin add/edit/delete events and toggle the blackout (closure) for the day.
     Staff also see the day's enrollment activity (day.staff — the service only populates
     enrollments for staff). Reached by tapping a day in calendar/month. --%>
<g:if test="${day}">
<%
    def dayVals    = { iso -> '{"template": "calendar/day", "data[day]": "service:scheduleService:dayView", "data[user]": "currentUser", "iso": "' + iso + '"}' }
    def thisDayVals = '{"template": "calendar/day", "data[day]": "service:scheduleService:dayView", "data[user]": "currentUser", "iso": "' + day.iso + '"}'
%>
<div>
    <div class="flex justify-center mb-4">
        <g:render template="/universal/calendar/calToggle" model="[active: 'day', year: day.year, month: day.month, iso: day.iso]"/>
    </div>

    <div class="flex items-center justify-between gap-2 mb-4">
        <button class="h-10 w-10 rounded-full hover:bg-cream-100 text-stone-600 text-lg shrink-0" aria-label="Previous day"
                hx-get="/universal/showView" hx-vals='${dayVals(day.prevIso)}' hx-target="#content" hx-swap="innerHTML">&lsaquo;</button>
        <div class="text-center min-w-0">
            <h2 class="text-lg font-bold text-stone-800 truncate">${day.label}</h2>
            <g:if test="${day.isToday}"><span class="text-xs font-semibold text-rose-600">Today</span></g:if>
        </div>
        <button class="h-10 w-10 rounded-full hover:bg-cream-100 text-stone-600 text-lg shrink-0" aria-label="Next day"
                hx-get="/universal/showView" hx-vals='${dayVals(day.nextIso)}' hx-target="#content" hx-swap="innerHTML">&rsaquo;</button>
    </div>

    <%-- Blackout (closure) banner — visible to everyone; staff can reopen or mark closed. --%>
    <g:if test="${day.blackout}">
        <div class="mb-4 flex items-center justify-between gap-2 rounded-xl bg-stone-100 border border-stone-200 px-3 py-2">
            <span class="text-sm text-stone-600 min-w-0 truncate"><g:render template="/universal/components/pill" model="[label: 'Closed', tone: 'stone']"/> ${day.blackout.reason ?: 'School closed'}</span>
            <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
                <button class="btn-secondary btn-sm shrink-0"
                        hx-post="/universal/delete/${day.blackout.id}?domainName=BlackoutDate" hx-vals='${thisDayVals}'
                        hx-target="#content" hx-swap="innerHTML" hx-confirm="Reopen this day?">Reopen</button>
            </sec:ifAnyGranted>
        </div>
    </g:if>
    <g:else>
        <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
            <form hx-post="/universal/save?domainName=BlackoutDate" hx-vals='${thisDayVals}'
                  hx-target="#content" hx-swap="innerHTML"
                  class="mb-4 flex items-center gap-2">
                <input type="hidden" name="blackoutDate" value="${day.iso}"/>
                <input type="text" name="reason" placeholder="Reason (holiday, closure…)"
                       class="flex-1 min-w-0 px-3 py-2 border border-stone-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-rose-500"/>
                <button type="submit" class="btn-secondary btn-sm shrink-0 min-h-[44px]">Mark closed</button>
            </form>
        </sec:ifAnyGranted>
    </g:else>

    <div class="flex justify-end mb-4">
        <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
            <button class="btn-cta btn-sm shrink-0" hx-get="/universal/showView"
                    hx-vals='{"template": "events/form", "day": "${day.iso}"}'
                    hx-target="#content" hx-swap="innerHTML">+ Add event</button>
        </sec:ifAnyGranted>
    </div>

    <g:if test="${day.events}">
        <div class="space-y-2">
            <g:each in="${day.events}" var="ev">
                <div class="card p-4 flex items-start gap-3">
                    <span class="mt-1 h-3 w-3 rounded-full shrink-0 ${ksh.eventDot(color: ev.color)}"></span>
                    <div class="min-w-0 flex-1">
                        <p class="text-xs font-semibold text-stone-500">${ev.allDay ? 'All day' : ev.time}</p>
                        <h3 class="font-semibold text-stone-800">${ev.title}</h3>
                        <g:if test="${ev.location}"><p class="text-xs text-stone-400">${ev.location}</p></g:if>
                        <g:if test="${ev.description}"><p class="mt-1 text-sm text-stone-600 whitespace-pre-line">${ev.description}</p></g:if>
                    </div>
                    <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
                        <div class="flex flex-col items-end gap-1 shrink-0">
                            <button class="text-xs font-semibold text-rose-700 hover:underline" hx-get="/universal/showView"
                                    hx-vals='{"template": "events/form", "data[item]": "get:Event:eId", "eId": "${ev.id}", "day": "${day.iso}"}'
                                    hx-target="#content" hx-swap="innerHTML">Edit</button>
                            <button class="btn-delete-link"
                                    hx-post="/universal/delete/${ev.id}?domainName=Event" hx-vals='${thisDayVals}'
                                    hx-target="#content" hx-swap="innerHTML" hx-confirm="Delete this event?">Delete</button>
                        </div>
                    </sec:ifAnyGranted>
                </div>
            </g:each>
        </div>
    </g:if>
    <g:else>
        <div class="card p-8 text-center">
            <p class="text-sm text-stone-500">Nothing scheduled for this day.</p>
            <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
                <p class="text-xs text-stone-400 mt-1">Tap “Add event” to put something on the calendar.</p>
            </sec:ifAnyGranted>
        </div>
    </g:else>

    <%-- Enrollment activity — staff-only layer (the service returns [] for students). --%>
    <g:if test="${day.staff && day.enrollments}">
        <div class="mt-6">
            <h3 class="text-sm font-bold text-stone-700 mb-2">Enrollments <g:render template="/universal/components/pill" model="[label: "${day.enrollments.size()}", tone: 'rose', size: 'xs']"/></h3>
            <div class="card divide-y divide-cream-200">
                <g:each in="${day.enrollments}" var="en">
                    <div class="px-4 py-3 flex items-center justify-between gap-3">
                        <span class="text-sm font-medium text-stone-700 truncate">${en.student ?: 'Student'}</span>
                        <span class="text-xs text-stone-400 truncate shrink-0">${en.course ?: ''}</span>
                    </div>
                </g:each>
            </div>
        </div>
    </g:if>
</div>
</g:if>
