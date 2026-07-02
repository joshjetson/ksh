<%-- A classroom's roster for the active term — a teacher's classroom hub. Model: classroom
     (get:Classroom:cId), memberships (filter:ClassroomMembership:classroom.id=cId,term.id=activeTerm),
     active (filter:Term:active=true → the current term), user. Enroll/unenroll students
     (search the school's users → create/delete ClassroomMembership). Attendance + grades next. --%>
<% def term = active ? active[0] : null; def tId = term?.id; String todayIso = new Date().format('yyyy-MM-dd') %>
<div class="max-w-xl mx-auto">
    <button class="text-stone-400 hover:text-rose-700 text-sm mb-4" hx-get="/universal/showView"
            hx-vals='{"template": "classrooms/myClassrooms", "data[assignments]": "list:ClassroomStaff", "data[user]": "currentUser"}'
            hx-target="#content" hx-swap="innerHTML">&larr; My Classroom</button>
    <div class="flex items-start justify-between gap-3 mb-4">
        <div class="min-w-0">
            <h2 class="text-xl font-bold text-stone-800"><g:if test="${classroom}">${classroom.name}</g:if></h2>
            <p class="text-sm text-stone-400"><g:if test="${term}">${term.name}</g:if><g:else>No active term — ask an admin to set one up</g:else></p>
        </div>
        <g:if test="${tId}">
            <button class="btn-secondary btn-sm shrink-0" hx-get="/universal/showView"
                    hx-vals='{"template": "classrooms/attendance", "data[classroom]": "get:Classroom:cId", "data[memberships]": "filter:ClassroomMembership:classroom.id=cId,term.id=activeTerm", "data[dayAttendance]": "filter:Attendance:day=day", "cId": "${classroom?.id}", "day": "${todayIso}", "data[user]": "currentUser"}'
                    hx-target="#content" hx-swap="innerHTML">Take attendance</button>
        </g:if>
    </div>

    <div id="enroll-list">
        <g:render template="/universal/classrooms/enrollRows" model="[memberships: memberships, classroom: classroom, active: active]"/>
    </div>

    <g:if test="${tId}">
        <div class="mt-5 pt-5 border-t border-cream-200">
            <p class="text-sm font-semibold text-stone-700 mb-2">Enroll a student</p>
            <g:render template="/universal/components/searchBar"
                      model="[target: 'enroll-results', name: 'searchTerm', placeholder: 'Search students by name…',
                              vals: '{\"template\": \"classrooms/enrollSearch\", \"data[res]\": \"search:User:name,username:searchTerm\", \"cId\": \"' + classroom?.id + '\", \"tId\": \"' + tId + '\"}']"/>
            <div id="enroll-results"></div>
        </div>
    </g:if>
</div>
