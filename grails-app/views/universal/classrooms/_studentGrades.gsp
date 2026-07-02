<%-- A student's grades + progress for the active term (teacher view). Model: student
     (get:User:uId), grades (filter:Grade:user.id=uId,term.id=activeTerm), sem
     (filter:Term:active=true), user. params: uId, cId (for the back-to-roster link). --%>
<%
    def cId = params.cId; def uId = student?.id
    def t = sem ? sem[0] : null; def tId = t?.id
    String selCls = 'block w-full px-3 py-2 rounded-xl bg-white border border-cream-300 text-stone-800 focus:border-rose-500 focus:ring-1 focus:ring-rose-500 focus:outline-none min-h-[44px] text-base'
    def rosterVals = '{"template": "classrooms/roster", "data[classroom]": "get:Classroom:cId", "data[active]": "filter:Term:active=true", "data[memberships]": "filter:ClassroomMembership:classroom.id=cId,term.id=activeTerm", "cId": "' + cId + '", "data[user]": "currentUser"}'
%>
<div class="max-w-xl mx-auto">
    <button class="text-stone-400 hover:text-rose-700 text-sm mb-4" hx-get="/universal/showView" hx-vals='${rosterVals}' hx-target="#content" hx-swap="innerHTML">&larr; Roster</button>

    <div class="flex items-center gap-3 mb-4">
        <g:render template="/universal/components/avatar" model="[name: (student?.name ?: student?.username), size: 'h-12 w-12', textSize: 'text-base']"/>
        <div class="min-w-0">
            <h2 class="text-xl font-bold text-stone-800 truncate"><g:if test="${student}">${student.name ?: student.username}</g:if></h2>
            <p class="text-sm text-stone-400"><g:if test="${t}">${t.name}</g:if></p>
        </div>
    </div>

    <div id="grade-list">
        <g:render template="/universal/classrooms/gradeRows" model="[grades: grades, student: student, sem: sem]"/>
    </div>

    <g:if test="${tId}">
        <div class="card p-4 mt-4">
            <p class="text-sm font-semibold text-stone-700 mb-3">Record a grade</p>
            <form hx-post="/universal/save?domainName=Grade"
                  hx-vals='{"template": "classrooms/gradeRows", "data[grades]": "filter:Grade:user.id=uId,term.id=activeTerm", "data[student]": "get:User:uId", "data[sem]": "filter:Term:active=true", "uId": "${uId}"}'
                  hx-target="#grade-list" hx-swap="innerHTML" class="space-y-3"
                  hx-on::after-request="if(event.detail.successful) this.reset()">
                <input type="hidden" name="user.id" value="${uId}"/>
                <input type="hidden" name="term.id" value="${tId}"/>
                <div class="grid grid-cols-2 gap-3">
                    <div>
                        <label for="g-subject" class="block text-xs font-medium text-stone-500 mb-1">Subject</label>
                        <select id="g-subject" name="subject" class="${selCls}">
                            <g:each in="${['Speaking', 'Listening', 'Reading', 'Writing', 'Grammar', 'Vocabulary', 'Culture']}" var="subj"><option>${subj}</option></g:each>
                        </select>
                    </div>
                    <div>
                        <label for="g-period" class="block text-xs font-medium text-stone-500 mb-1">Period</label>
                        <select id="g-period" name="period" class="${selCls}">
                            <g:each in="${['Q1', 'Q2', 'Q3', 'Q4', 'Midterm', 'Final']}" var="p"><option>${p}</option></g:each>
                        </select>
                    </div>
                </div>
                <g:render template="/universal/components/input" model="[name: 'score', label: 'Score (0–100)', type: 'number', required: true]"/>
                <g:render template="/universal/components/textarea" model="[name: 'notes', label: 'Note (optional)', rows: 2]"/>
                <g:render template="/universal/components/button" model="[text: 'Save grade', type: 'submit', variant: 'cta', full: true]"/>
            </form>
        </div>
    </g:if>
</div>
