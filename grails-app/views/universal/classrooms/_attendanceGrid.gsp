<%-- Classroom attendance grid for one day. Model: memberships (List<ClassroomMembership>),
     dayAttendance (List<Attendance> for the day). params: cId, day. Selects pre-fill from
     existing attendance; Save batch-upserts (naturalKey user,day) and re-renders the screen. --%>
<%
    def cId = params.cId; def day = params.day
    def roster = (memberships ?: []).findAll { it.user }.sort { (it.user.name ?: it.user.username).toLowerCase() }
    def statuses = ['present', 'absent', 'tardy', 'excused']
    def byUser = [:]; (dayAttendance ?: []).each { if (it.user) byUser[it.user.id] = it.status }
    String selCls = 'att-status px-3 py-2 rounded-xl bg-white border border-cream-300 text-stone-800 focus:border-rose-500 focus:ring-1 focus:ring-rose-500 focus:outline-none min-h-[44px] text-sm'
%>
<g:if test="${roster && day}">
<div id="classroom-att-roster" data-day="${day}" data-cid="${cId}">
    <div class="space-y-2">
        <g:each in="${roster}" var="m">
            <% String cur = byUser[m.user.id] ?: 'present' %>
            <div class="card p-3 flex items-center justify-between gap-3">
                <span class="font-medium text-stone-800 truncate">${m.user.name ?: m.user.username}</span>
                <select data-user="${m.user.id}" class="${selCls}">
                    <g:each in="${statuses}" var="st"><option value="${st}" ${cur == st ? 'selected' : ''}>${st.capitalize()}</option></g:each>
                </select>
            </div>
        </g:each>
    </div>
    <div class="mt-4 flex justify-end">
        <button type="button" class="btn-cta" onclick="kshSaveClassroomAttendance()">Save attendance</button>
    </div>
</div>
<script>
    window.kshSaveClassroomAttendance = function () {
        var root = document.getElementById('classroom-att-roster');
        var day = root.getAttribute('data-day'), cId = root.getAttribute('data-cid');
        var records = [];
        root.querySelectorAll('.att-status').forEach(function (sel) {
            records.push({ user: sel.getAttribute('data-user'), day: day, status: sel.value });
        });
        if (!records.length) return;
        kshSaveBatch('Attendance', 'user,day', records, {
            template: 'classrooms/attendance',
            'data[classroom]': 'get:Classroom:cId',
            'data[memberships]': 'filter:ClassroomMembership:classroom.id=cId,term.id=activeTerm',
            'data[dayAttendance]': 'filter:Attendance:day=day',
            cId: cId, day: day
        });
    };
</script>
</g:if>
<g:else>
    <p class="text-sm text-stone-400 text-center py-10">No students enrolled for this term yet.</p>
</g:else>
