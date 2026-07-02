<%-- A teacher's assigned classrooms. Model: assignments (list:ClassroomStaff — scoped to the
     current teacher by READ_SCOPE_FIELDS, so only their own), user. Tap a classroom → roster. --%>
<div class="max-w-xl mx-auto">
    <div class="mb-5">
        <h2 class="text-xl font-bold text-stone-800">My Classroom</h2>
        <p class="text-sm text-stone-400">Your assigned classrooms.</p>
    </div>
    <g:if test="${assignments}">
        <div class="space-y-2">
            <g:each in="${assignments}" var="a">
                <button type="button" class="card p-4 w-full text-left hover:bg-cream-50 flex items-center justify-between gap-3"
                        hx-get="/universal/showView"
                        hx-vals='{"template": "classrooms/roster", "data[classroom]": "get:Classroom:cId", "data[active]": "filter:Term:active=true", "data[memberships]": "filter:ClassroomMembership:classroom.id=cId,term.id=activeTerm", "cId": "${a.classroom.id}", "data[user]": "currentUser"}'
                        hx-target="#content" hx-swap="innerHTML">
                    <span class="min-w-0">
                        <span class="block font-semibold text-stone-800 truncate">${a.classroom.name}</span>
                        <g:if test="${a.classroom.description}"><span class="block text-sm text-stone-400 truncate">${a.classroom.description}</span></g:if>
                    </span>
                    <span class="text-stone-300">&rsaquo;</span>
                </button>
            </g:each>
        </div>
    </g:if>
    <g:else>
        <g:render template="/universal/components/emptyState"
                  model="[message: 'No classroom assigned yet', hint: 'An admin can assign you a classroom from the Classrooms screen.']"/>
    </g:else>
</div>
