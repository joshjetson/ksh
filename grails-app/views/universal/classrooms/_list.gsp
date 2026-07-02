<%-- Classrooms (live cohorts / groups). Model: items (List<Classroom>), user. Admin manages. --%>
<% def rows = (items ?: []).sort { it.sortOrder ?: 0 } %>
<div>
    <div class="flex items-center justify-between mb-5">
        <div>
            <h2 class="text-xl font-bold text-stone-800">Classrooms</h2>
            <p class="text-sm text-stone-400">Live class cohorts &amp; groups.</p>
        </div>
        <sec:ifAnyGranted roles="ROLE_ADMIN">
            <button class="btn-cta" hx-get="/universal/showView" hx-vals='{"template": "classrooms/form"}'
                    hx-target="#content" hx-swap="innerHTML">+ New</button>
        </sec:ifAnyGranted>
    </div>

    <g:if test="${rows}">
        <div class="space-y-3">
            <g:each in="${rows}" var="c">
                <div class="card p-4 flex items-center justify-between gap-3">
                    <div class="min-w-0">
                        <p class="font-semibold text-stone-800">${c.name}</p>
                        <g:if test="${c.description}"><p class="text-sm text-stone-500">${c.description}</p></g:if>
                    </div>
                    <sec:ifAnyGranted roles="ROLE_ADMIN">
                        <div class="flex flex-wrap items-center gap-2 justify-end shrink-0">
                            <button class="btn-secondary btn-sm" hx-get="/universal/showView"
                                    hx-vals='{"template": "classrooms/roster", "data[classroom]": "get:Classroom:cId", "data[active]": "filter:Term:active=true", "data[memberships]": "filter:ClassroomMembership:classroom.id=cId,term.id=activeTerm", "cId": "${c.id}", "data[user]": "currentUser"}'
                                    hx-target="#content" hx-swap="innerHTML">Roster</button>
                            <button class="btn-secondary btn-sm" hx-get="/universal/showView"
                                    hx-vals='{"template": "classrooms/form", "data[item]": "get:Classroom:cId", "cId": "${c.id}"}'
                                    hx-target="#content" hx-swap="innerHTML">Edit</button>
                            <button class="btn-secondary btn-sm" hx-get="/universal/showView"
                                    hx-vals='{"template": "classrooms/teachers", "data[classroom]": "get:Classroom:cId", "data[teachers]": "filter:ClassroomStaff:classroom.id=cId", "cId": "${c.id}"}'
                                    hx-target="#content" hx-swap="innerHTML">Teachers</button>
                            <button class="btn-delete"
                                    hx-post="/universal/delete/${c.id}?domainName=Classroom"
                                    hx-vals='{"template": "classrooms/list", "data[items]": "list:Classroom", "data[user]": "currentUser"}'
                                    hx-target="#content" hx-swap="innerHTML" hx-confirm="Delete ${c.name}?">Delete</button>
                        </div>
                    </sec:ifAnyGranted>
                </div>
            </g:each>
        </div>
    </g:if>
    <g:else>
        <g:render template="/universal/components/emptyState" model="[message: 'No classrooms yet']"/>
    </g:else>
</div>
