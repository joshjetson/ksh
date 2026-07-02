<div class="bg-gradient-to-r from-rose-500 to-rose-700 rounded-xl p-6 mb-6 text-white">
    <h2 class="text-2xl font-bold">Welcome back, ${stats?.name ?: 'learner'}! 🇰🇷</h2>
    <p class="text-rose-50 text-sm mt-1">Pick up where you left off and keep your streak going.</p>
</div>

<g:if test="${pinned}">
    <div class="space-y-3 mb-6">
        <g:each in="${pinned}" var="a">
            <button hx-get="/universal/showView"
                    hx-vals='{"template": "announcements/list", "data[user]": "currentUser", "data[items]": "list:Announcement"}'
                    hx-target="#content" hx-swap="innerHTML"
                    class="card w-full p-4 text-left flex items-start gap-3 hover:border-gold-400 transition-colors min-h-[44px]">
                <span class="text-lg" aria-hidden="true">📌</span>
                <span class="min-w-0">
                    <span class="block font-semibold text-stone-800 truncate">${a.title}</span>
                    <span class="block text-sm text-stone-500 line-clamp-2">${a.body}</span>
                </span>
            </button>
        </g:each>
    </div>
</g:if>

<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
    <g:render template="/universal/components/statCard" model="[value: stats?.enrollments?.total ?: 0, label: 'My courses']"/>
    <g:render template="/universal/components/statCard" model="[value: stats?.enrollments?.completed ?: 0, label: 'Completed']"/>
    <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-4">
        <div class="text-2xl font-bold text-stone-800">${stats?.enrollments?.avgProgress ?: 0}%</div>
        <div class="text-xs text-stone-500 mt-1">Avg. progress</div>
    </div>
    <g:render template="/universal/components/statCard" model="[value: stats?.badges ?: 0, label: 'Badges earned']"/>
</div>

<g:if test="${myClasses}">
    <div class="card p-5 mb-6">
        <h3 class="text-sm font-semibold text-stone-700 mb-3">My classes this term</h3>
        <div class="space-y-2">
            <g:each in="${myClasses}" var="m">
                <div class="flex items-center justify-between gap-3">
                    <span class="text-sm font-medium text-stone-700 truncate">${m.classroom?.name}</span>
                    <g:render template="/universal/components/pill" model="[label: m.term?.name, tone: 'cream']"/>
                </div>
            </g:each>
        </div>
        <g:if test="${myGrades}">
            <div class="border-t border-cream-200 mt-4 pt-3">
                <h4 class="text-xs font-semibold text-stone-400 uppercase tracking-wide mb-2">Recent grades</h4>
                <div class="flex flex-wrap gap-2">
                    <g:each in="${myGrades}" var="g">
                        <%-- tone computed in a scriptlet: '>' inside a tag attribute breaks the GSP parser --%>
                        <% def gradeTone = g.score >= 80 ? 'emerald' : (g.score >= 60 ? 'gold' : 'rose') %>
                        <g:render template="/universal/components/pill" model="[label: g.subject + ' ' + g.period + ': ' + g.score + '%', tone: gradeTone]"/>
                    </g:each>
                </div>
            </div>
        </g:if>
    </div>
</g:if>

<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
    <button hx-get="/universal/showView"
            hx-vals='{"template": "courses/myCourses", "data[user]": "currentUser", "data[enrollments]": "filter:CourseEnrollment:user.id=currentUserId"}'
            hx-target="#content"
            hx-swap="innerHTML"
            class="bg-white rounded-xl shadow-sm border border-stone-200 p-5 text-left hover:border-rose-300 transition-colors min-h-[44px]">
        <div class="font-semibold text-stone-800">Continue learning →</div>
        <div class="text-sm text-stone-500 mt-1">Jump back into your courses</div>
    </button>
    <button hx-get="/universal/showView"
            hx-vals='{"template": "messaging/home", "data[list]": "service:messageService:channelList", "data[user]": "currentUser"}'
            hx-target="#content"
            hx-swap="innerHTML"
            class="bg-white rounded-xl shadow-sm border border-stone-200 p-5 text-left hover:border-rose-300 transition-colors min-h-[44px]">
        <div class="font-semibold text-stone-800">Message the school →</div>
        <div class="text-sm text-stone-500 mt-1">${stats?.messages ?: 0} messages in your conversations</div>
    </button>
</div>
