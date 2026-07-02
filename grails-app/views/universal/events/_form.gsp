<%-- New / edit event. Model: item (Event, optional). `day` param (yyyy-MM-dd, passed by
     the calendar day view) prefills the date and is where we return after saving.
     datetime-local binds via grails.databinding.dateFormats. --%>
<%
    def editing  = item?.id != null
    def action   = editing ? "/universal/update/${item.id}?domainName=Event" : "/universal/save?domainName=Event"
    String dayIso = params.day
    def back = dayIso
        ? '{"template": "calendar/day", "data[day]": "service:scheduleService:dayView", "data[user]": "currentUser", "iso": "' + dayIso + '"}'
        : '{"template": "calendar/month", "data[cal]": "service:scheduleService:monthView", "data[user]": "currentUser"}'
    String startsStr = item?.startsAt ? formatDate(date: item.startsAt, format: "yyyy-MM-dd'T'HH:mm")
                       : (dayIso ? "${dayIso}T09:00" : '')
    String endsStr   = item?.endsAt   ? formatDate(date: item.endsAt,   format: "yyyy-MM-dd'T'HH:mm") : ''
    String curColor  = item?.color ?: 'rose'
%>
<div class="max-w-lg">
    <button class="text-stone-400 hover:text-rose-700 text-sm mb-4" hx-get="/universal/showView" hx-vals='${back}'
            hx-target="#content" hx-swap="innerHTML">&larr; Back</button>
    <div class="card p-6">
        <h2 class="text-xl font-bold text-stone-800 mb-5">${editing ? 'Edit event' : 'New event'}</h2>
        <form hx-post="${action}" hx-vals='${back}' hx-target="#content" hx-swap="innerHTML" class="space-y-4">
            <g:render template="/universal/components/input"
                      model="[name: 'title', label: 'Title', required: true, value: item?.title]"/>
            <div class="grid grid-cols-2 gap-3">
                <g:render template="/universal/components/input"
                          model="[name: 'startsAt', label: 'Starts', type: 'datetime-local', required: true, value: startsStr]"/>
                <g:render template="/universal/components/input"
                          model="[name: 'endsAt', label: 'Ends', type: 'datetime-local', value: endsStr]"/>
            </div>
            <g:render template="/universal/components/input"
                      model="[name: 'location', label: 'Location', value: item?.location]"/>
            <g:render template="/universal/components/textarea"
                      model="[name: 'description', label: 'Details', rows: 3, value: item?.description]"/>

            <div>
                <label class="block text-sm font-medium text-stone-700 mb-1">Color</label>
                <div class="flex items-center gap-3">
                    <g:each in="${['rose', 'gold', 'sky', 'amber', 'emerald', 'violet']}" var="ck">
                        <label class="cursor-pointer">
                            <input type="radio" name="color" value="${ck}" class="peer sr-only" ${curColor == ck ? 'checked' : ''}/>
                            <span class="block h-8 w-8 rounded-full ring-2 ring-offset-2 ring-transparent peer-checked:ring-stone-700 ${ksh.eventDot(color: ck)}"></span>
                        </label>
                    </g:each>
                </div>
            </div>

            <label class="flex items-center gap-2 text-sm text-stone-700">
                <input type="hidden" name="_allDay"/>
                <input type="checkbox" name="allDay" value="true" ${item?.allDay ? 'checked' : ''}
                       class="h-4 w-4 rounded border-cream-300 text-rose-600 focus:ring-rose-500"/>
                All-day event
            </label>
            <g:render template="/universal/components/button"
                      model="[text: editing ? 'Save' : 'Add event', type: 'submit', variant: 'primary', full: true]"/>
        </form>
    </div>
</div>
