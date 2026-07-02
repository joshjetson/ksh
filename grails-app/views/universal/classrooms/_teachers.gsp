<%-- Admin: manage which staff teach a classroom. Model: classroom (get:Classroom:cId),
     teachers (filter:ClassroomStaff:classroom.id=cId). Add via searchPeople (the
     ClassroomStaff validator rejects non-teacher users); remove via generic delete.
     Admin-only surface. --%>
<% def cId = params.cId %>
<div class="max-w-lg">
    <button class="text-stone-400 hover:text-rose-700 text-sm mb-4" hx-get="/universal/showView"
            hx-vals='{"template": "classrooms/list", "data[items]": "list:Classroom", "data[user]": "currentUser"}'
            hx-target="#content" hx-swap="innerHTML">&larr; Classrooms</button>
    <div class="card p-6">
        <h2 class="text-xl font-bold text-stone-800 mb-1">Teachers</h2>
        <p class="text-sm text-stone-400 mb-5"><g:if test="${classroom}">${classroom.name} &mdash; </g:if>staff assigned to this classroom.</p>
        <div id="teacher-list">
            <g:render template="/universal/classrooms/teacherRows" model="[teachers: teachers, classroom: classroom]"/>
        </div>
        <div class="mt-5 pt-5 border-t border-cream-200">
            <p class="text-sm font-semibold text-stone-700 mb-2">Assign a teacher</p>
            <g:render template="/universal/components/searchBar"
                      model="[target: 'teacher-add-results', placeholder: 'Search staff by name…',
                              vals: '{\"template\": \"classrooms/teacherSearch\", \"data[res]\": \"service:messageService:searchPeople\", \"cId\": \"' + cId + '\"}']"/>
            <div id="teacher-add-results"></div>
        </div>
    </div>
</div>
