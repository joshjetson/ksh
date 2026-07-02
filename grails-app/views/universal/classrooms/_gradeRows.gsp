<%-- Grade list + basic progress metrics (re-rendered into #grade-list). Model: grades
     (List<Grade>), student (User), sem (List<Term>). Metrics (overall + per-subject
     averages) are computed in-view — no aggregation service needed. --%>
<%
    def uId = student?.id
    def gs = (grades ?: []).findAll { it.score != null }
    def overall = gs ? Math.round((gs*.score.sum() as double) / gs.size()) : null
    def bySubj = gs.groupBy { it.subject }
                   .collect { k, v -> [subject: k, avg: Math.round((v*.score.sum() as double) / v.size()), count: v.size()] }
                   .sort { it.subject }
    def sorted = gs.sort { a, b -> (b.dateCreated ?: new Date(0)) <=> (a.dateCreated ?: new Date(0)) }
    def tone = { sc -> sc >= 80 ? 'emerald' : sc >= 60 ? 'gold' : 'rose' }
    def scoreCls = { sc -> sc >= 80 ? 'text-emerald-700' : sc >= 60 ? 'text-gold-700' : 'text-rose-600' }
%>
<g:if test="${gs}">
    <div class="card p-4 mb-3">
        <div class="flex items-baseline justify-between">
            <span class="text-sm font-semibold text-stone-700">Progress</span>
            <span class="text-2xl font-bold text-rose-700">${overall}%<span class="text-sm font-medium text-stone-400"> avg</span></span>
        </div>
        <div class="mt-3 space-y-1.5">
            <g:each in="${bySubj}" var="m">
                <div class="flex items-center justify-between text-sm">
                    <span class="font-medium text-stone-600">${m.subject}</span>
                    <span class="flex items-center gap-2">
                        <g:render template="/universal/components/pill" model="[label: m.avg + '%', tone: tone(m.avg)]"/>
                        <span class="text-xs text-stone-400">${m.count} grade${m.count == 1 ? '' : 's'}</span>
                    </span>
                </div>
            </g:each>
        </div>
    </div>
    <div class="space-y-2">
        <g:each in="${sorted}" var="g">
            <div class="card p-3 flex items-center justify-between gap-3">
                <div class="min-w-0">
                    <p class="font-semibold text-stone-800">${g.subject} <span class="text-xs font-normal text-stone-400">&middot; ${g.period}</span></p>
                    <g:if test="${g.notes}"><p class="text-xs text-stone-500 truncate">${g.notes}</p></g:if>
                </div>
                <div class="flex items-center gap-3 shrink-0">
                    <span class="text-lg font-bold ${scoreCls(g.score)}">${g.score}%</span>
                    <button class="text-stone-300 hover:text-rose-600 text-xl leading-none min-h-[44px] px-1" aria-label="Delete grade"
                            hx-post="/universal/delete/${g.id}?domainName=Grade"
                            hx-vals='{"template": "classrooms/gradeRows", "data[grades]": "filter:Grade:user.id=uId,term.id=activeTerm", "data[student]": "get:User:uId", "data[sem]": "filter:Term:active=true", "uId": "${uId}"}'
                            hx-target="#grade-list" hx-swap="innerHTML" hx-confirm="Delete this grade?">&times;</button>
                </div>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <p class="text-sm text-stone-400">No grades recorded yet — add one below.</p>
</g:else>
