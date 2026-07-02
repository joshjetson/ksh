<%-- Enroll add-search results. Model: res (search:User:name,username:searchTerm → List<User>).
     Reads params.cId + params.tId. Each result enrolls the student in this classroom for the
     active term (generic save ClassroomMembership); the unique constraint blocks a duplicate. --%>
<% def cId = params.cId; def tId = params.tId %>
<g:if test="${res}">
    <div class="mt-3 space-y-2">
        <g:each in="${res}" var="u">
            <div class="flex items-center justify-between gap-3 rounded-xl border border-cream-200 px-3 py-2">
                <div class="flex items-center gap-2 min-w-0">
                    <g:render template="/universal/components/avatar" model="[name: (u.name ?: u.username), size: 'h-8 w-8', textSize: 'text-xs']"/>
                    <span class="font-medium text-stone-800 truncate">${u.name ?: u.username}</span>
                </div>
                <button class="btn-secondary btn-sm shrink-0" hx-post="/universal/save?domainName=ClassroomMembership"
                        hx-vals='{"template": "classrooms/enrollRows", "data[memberships]": "filter:ClassroomMembership:classroom.id=cId,term.id=activeTerm", "data[classroom]": "get:Classroom:cId", "data[active]": "filter:Term:active=true", "cId": "${cId}", "user.id": "${u.id}", "classroom.id": "${cId}", "term.id": "${tId}"}'
                        hx-target="#enroll-list" hx-swap="innerHTML">Enroll</button>
            </div>
        </g:each>
    </div>
</g:if>
<g:elseif test="${params.searchTerm}">
    <p class="mt-3 text-sm text-stone-400">No students match &ldquo;${params.searchTerm}&rdquo;.</p>
</g:elseif>
