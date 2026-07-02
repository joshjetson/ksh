<%-- Attendance roster grid. Model: memberships (ClassroomMembership rows for the chosen
     classroom + active term). Reads params.day. Save batch-upserts Attendance
     (naturalKey user,day) via kshSaveBatch. --%>
<%
    def roster = (memberships ?: []).findAll { it.user }.sort { (it.user.name ?: it.user.username).toLowerCase() }
    String day = params.day ?: ''
    def statuses = ['present', 'absent', 'tardy', 'excused']
%>
<g:if test="${roster && day}">
<div id="att-roster" data-day="${day}">
    <div class="space-y-2">
        <g:each in="${roster}" var="m">
            <div class="card p-3 flex items-center justify-between gap-3">
                <span class="font-medium text-stone-800">${m.user.name ?: m.user.username}</span>
                <select data-user="${m.user.id}"
                        class="att-status px-3 py-2 rounded-xl bg-white border border-cream-300 text-stone-800 focus:border-rose-500 focus:ring-1 focus:ring-rose-500 focus:outline-none min-h-[44px] text-sm">
                    <g:each in="${statuses}" var="st"><option value="${st}">${st.capitalize()}</option></g:each>
                </select>
            </div>
        </g:each>
    </div>
    <div class="mt-4 flex justify-end">
        <button type="button" class="btn-cta" onclick="kshSaveAttendance()">Save attendance</button>
    </div>
</div>
<script>
    window.kshSaveAttendance = function () {
        var root = document.getElementById('att-roster');
        var day = root.getAttribute('data-day');
        var records = [];
        root.querySelectorAll('.att-status').forEach(function (sel) {
            records.push({ user: sel.getAttribute('data-user'), day: day, status: sel.value });
        });
        if (!records.length) return;
        kshSaveBatch('Attendance', 'user,day', records, {
            template: 'attendance/record', 'data[classrooms]': 'list:Classroom', 'data[user]': 'currentUser'
        });
    };
</script>
</g:if>
<g:elseif test="${!day}">
    <p class="text-sm text-rose-600 text-center py-6">Pick a day first.</p>
</g:elseif>
<g:else>
    <p class="text-sm text-stone-400 text-center py-10">No students in this classroom yet.</p>
</g:else>
