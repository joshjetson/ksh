<%-- A classroom's assigned teachers (re-rendered into #teacher-list on assign/remove). Model:
     teachers (List<ClassroomStaff>), classroom (Classroom). --%>
<% def cId = classroom?.id %>
<g:if test="${teachers}">
    <div class="space-y-2">
        <g:each in="${teachers}" var="t">
            <div class="flex items-center justify-between gap-3 rounded-xl border border-cream-200 px-3 py-2">
                <div class="flex items-center gap-2 min-w-0">
                    <g:render template="/universal/components/avatar" model="[name: (t.staff.name ?: t.staff.username), size: 'h-8 w-8', textSize: 'text-xs']"/>
                    <span class="font-medium text-stone-800 truncate">${t.staff.name ?: t.staff.username}</span>
                </div>
                <button class="btn-delete-link shrink-0" hx-post="/universal/delete/${t.id}?domainName=ClassroomStaff"
                        hx-vals='{"template": "classrooms/teacherRows", "data[teachers]": "filter:ClassroomStaff:classroom.id=cId", "data[classroom]": "get:Classroom:cId", "cId": "${cId}"}'
                        hx-target="#teacher-list" hx-swap="innerHTML" hx-confirm="Unassign this teacher?">Remove</button>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <p class="text-sm text-stone-400">No teachers assigned yet.</p>
</g:else>
