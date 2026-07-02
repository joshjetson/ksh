<%-- Take / check attendance for a classroom's enrolled roster on a day (teacher). Model:
     classroom (get:Classroom:cId), memberships
     (filter:ClassroomMembership:classroom.id=cId,term.id=activeTerm), dayAttendance
     (filter:Attendance:day=day → that day's records, manager-read), user.
     params: cId, day. Changing the day reloads just the grid. --%>
<%
    def cId = params.cId
    String today = new Date().format('yyyy-MM-dd')
    String day = params.day ?: today
    String inputCls = 'block w-full px-3 py-2 rounded-xl bg-white border border-cream-300 text-stone-800 focus:border-rose-500 focus:ring-1 focus:ring-rose-500 focus:outline-none min-h-[44px] text-base'
    def rosterVals = '{"template": "classrooms/roster", "data[classroom]": "get:Classroom:cId", "data[active]": "filter:Term:active=true", "data[memberships]": "filter:ClassroomMembership:classroom.id=cId,term.id=activeTerm", "cId": "' + cId + '", "data[user]": "currentUser"}'
%>
<div class="max-w-xl mx-auto">
    <button class="text-stone-400 hover:text-rose-700 text-sm mb-4" hx-get="/universal/showView" hx-vals='${rosterVals}' hx-target="#content" hx-swap="innerHTML">&larr; Roster</button>
    <div class="mb-4">
        <h2 class="text-xl font-bold text-stone-800">Attendance<g:if test="${classroom}"> &middot; ${classroom.name}</g:if></h2>
        <p class="text-sm text-stone-400">Mark today, or pick another day to take or review attendance.</p>
    </div>

    <div class="card p-4 mb-4">
        <label for="att-day" class="block text-sm font-medium text-stone-700 mb-1">Day</label>
        <input id="att-day" name="day" type="date" value="${day}" class="${inputCls}"
               hx-get="/universal/showView"
               hx-vals='{"template": "classrooms/attendanceGrid", "data[memberships]": "filter:ClassroomMembership:classroom.id=cId,term.id=activeTerm", "data[dayAttendance]": "filter:Attendance:day=day", "cId": "${cId}"}'
               hx-include="#att-day" hx-target="#classroom-att-grid" hx-swap="innerHTML" hx-trigger="change"/>
    </div>

    <div id="classroom-att-grid">
        <g:render template="/universal/classrooms/attendanceGrid" model="[memberships: memberships, dayAttendance: dayAttendance]"/>
    </div>
</div>
