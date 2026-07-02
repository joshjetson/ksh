<%-- Enrolled students in a classroom for the active term (re-rendered into #enroll-list on
     enroll/unenroll). Model: memberships (List<ClassroomMembership>), classroom (Classroom),
     active (List<Term>). --%>
<%
    def cId = classroom?.id
    def rows = (memberships ?: []).findAll { it.user }.sort { (it.user.name ?: it.user.username).toLowerCase() }
%>
<p class="text-sm text-stone-400 mb-2">${rows.size()} student${rows.size() == 1 ? '' : 's'} enrolled</p>
<g:if test="${rows}">
    <div class="space-y-2">
        <g:each in="${rows}" var="m">
            <div class="card p-3 flex items-center justify-between gap-3">
                <button type="button" class="flex items-center gap-3 min-w-0 flex-1 text-left"
                        hx-get="/universal/showView"
                        hx-vals='{"template": "classrooms/studentGrades", "data[student]": "get:User:uId", "data[grades]": "filter:Grade:user.id=uId,term.id=activeTerm", "data[sem]": "filter:Term:active=true", "uId": "${m.user.id}", "cId": "${cId}", "data[user]": "currentUser"}'
                        hx-target="#content" hx-swap="innerHTML">
                    <g:render template="/universal/components/avatar" model="[name: (m.user.name ?: m.user.username), size: 'h-9 w-9', textSize: 'text-sm']"/>
                    <span class="font-semibold text-stone-800 truncate">${m.user.name ?: m.user.username}</span>
                    <span class="text-stone-300 ml-1">&rsaquo;</span>
                </button>
                <button class="btn-delete-link shrink-0" hx-post="/universal/delete/${m.id}?domainName=ClassroomMembership"
                        hx-vals='{"template": "classrooms/enrollRows", "data[memberships]": "filter:ClassroomMembership:classroom.id=cId,term.id=activeTerm", "data[classroom]": "get:Classroom:cId", "data[active]": "filter:Term:active=true", "cId": "${cId}"}'
                        hx-target="#enroll-list" hx-swap="innerHTML" hx-confirm="Unenroll this student?">Remove</button>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <p class="text-sm text-stone-400">No students enrolled yet — search below to add some.</p>
</g:else>
