<%-- Attendance recorder (teacher/admin). Model: classrooms (List<Classroom>), user. Pick a
     classroom + day → load the roster → mark + save (batch upsert, natural key user,day). --%>
<% String today = new Date().format('yyyy-MM-dd') %>
<div>
    <div class="mb-5">
        <h2 class="text-xl font-bold text-stone-800">Attendance</h2>
        <p class="text-sm text-stone-400">Record a classroom's attendance for a day. Visible to teachers and admin only.</p>
    </div>

    <div class="card p-4 mb-4">
        <div class="grid sm:grid-cols-3 gap-3 items-end">
            <div>
                <label for="classroomId" class="block text-sm font-medium text-stone-700 mb-1">Classroom</label>
                <select id="classroomId" name="classroomId"
                        class="block w-full px-3 py-2 rounded-xl bg-white border border-cream-300 text-stone-800 focus:border-rose-500 focus:ring-1 focus:ring-rose-500 focus:outline-none min-h-[44px] text-base">
                    <option value="">— choose —</option>
                    <g:each in="${(classrooms ?: []).sort { it.sortOrder ?: 0 }}" var="c">
                        <option value="${c.id}">${c.name}</option>
                    </g:each>
                </select>
            </div>
            <div>
                <label for="day" class="block text-sm font-medium text-stone-700 mb-1">Day</label>
                <input id="day" name="day" type="date" value="${today}"
                       class="block w-full px-3 py-2 rounded-xl bg-white border border-cream-300 text-stone-800 focus:border-rose-500 focus:ring-1 focus:ring-rose-500 focus:outline-none min-h-[44px] text-base"/>
            </div>
            <button class="btn-secondary" hx-get="/universal/showView"
                    hx-vals='{"template": "attendance/grid", "data[memberships]": "filter:ClassroomMembership:classroom.id=classroomId,term.id=activeTerm"}'
                    hx-include="#classroomId,#day" hx-target="#att-grid" hx-swap="innerHTML">Load roster</button>
        </div>
    </div>

    <div id="att-grid">
        <p class="text-sm text-stone-400 text-center py-10">Choose a classroom and day, then load the roster.</p>
    </div>
</div>
