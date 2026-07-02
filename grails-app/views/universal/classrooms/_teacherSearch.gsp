<%-- Teacher add-search results. Model: res (searchPeople → people:[{userId,name}]). Reads
     params.cId. Assigning a non-teacher user is rejected by the ClassroomStaff validator. --%>
<% def cId = params.cId %>
<g:if test="${res?.people}">
    <div class="mt-3 space-y-2">
        <g:each in="${res.people}" var="p">
            <div class="flex items-center justify-between gap-3 rounded-xl border border-cream-200 px-3 py-2">
                <div class="flex items-center gap-2 min-w-0">
                    <g:render template="/universal/components/avatar" model="[name: p.name, size: 'h-8 w-8', textSize: 'text-xs']"/>
                    <span class="font-medium text-stone-800 truncate">${p.name}</span>
                </div>
                <button class="btn-secondary btn-sm shrink-0" hx-post="/universal/save?domainName=ClassroomStaff"
                        hx-vals='{"template": "classrooms/teacherRows", "data[teachers]": "filter:ClassroomStaff:classroom.id=cId", "data[classroom]": "get:Classroom:cId", "cId": "${cId}", "staff.id": "${p.userId}", "classroom.id": "${cId}"}'
                        hx-target="#teacher-list" hx-swap="innerHTML">Assign</button>
            </div>
        </g:each>
    </div>
</g:if>
<g:elseif test="${res?.q}">
    <p class="mt-3 text-sm text-stone-400">No staff match &ldquo;${res.q}&rdquo;.</p>
</g:elseif>
