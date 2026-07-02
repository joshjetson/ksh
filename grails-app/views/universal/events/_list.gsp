<%-- Calendar / events. Model: items (List<Event>), user. Read-only for students. --%>
<% def rows = (items ?: []).sort { it.startsAt } %>
<div>
    <div class="flex items-center justify-between mb-5">
        <div>
            <h2 class="text-xl font-bold text-stone-800">Calendar</h2>
            <p class="text-sm text-stone-400">School events and important dates.</p>
        </div>
        <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
            <button class="btn-cta" hx-get="/universal/showView" hx-vals='{"template": "events/form"}'
                    hx-target="#content" hx-swap="innerHTML">+ New</button>
        </sec:ifAnyGranted>
    </div>

    <g:if test="${rows}">
        <div class="space-y-3">
            <g:each in="${rows}" var="e">
                <div class="card p-4 flex items-start justify-between gap-3">
                    <div class="min-w-0">
                        <p class="text-xs font-semibold text-rose-700"><g:formatDate date="${e.startsAt}" format="EEE, MMM d, yyyy"/><g:if test="${!e.allDay}"> · <g:formatDate date="${e.startsAt}" format="h:mm a"/></g:if></p>
                        <h3 class="font-semibold text-stone-800">${e.title}</h3>
                        <g:if test="${e.location}"><p class="text-xs text-stone-400">${e.location}</p></g:if>
                        <g:if test="${e.description}"><p class="mt-1 text-sm text-stone-600 whitespace-pre-line">${e.description}</p></g:if>
                    </div>
                    <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
                        <div class="flex items-center gap-2 shrink-0">
                            <button class="btn-secondary btn-sm" hx-get="/universal/showView"
                                    hx-vals='{"template": "events/form", "data[item]": "get:Event:eId", "eId": "${e.id}"}'
                                    hx-target="#content" hx-swap="innerHTML">Edit</button>
                            <button class="btn-delete"
                                    hx-post="/universal/delete/${e.id}?domainName=Event"
                                    hx-vals='{"template": "events/list", "data[items]": "list:Event", "data[user]": "currentUser"}'
                                    hx-target="#content" hx-swap="innerHTML" hx-confirm="Delete this event?">Delete</button>
                        </div>
                    </sec:ifAnyGranted>
                </div>
            </g:each>
        </div>
    </g:if>
    <g:else>
        <g:render template="/universal/components/emptyState" model="[message: 'No events on the calendar yet']"/>
    </g:else>
</div>
