<g:if test="${stats}">

    <!-- Headline cards -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
        <g:render template="/universal/components/statCard"
                  model="[value: stats.enrollments.inPeriod, label: 'Enrollments', hint: stats.enrollments.all + ' all-time']"/>

        <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-4">
            <div class="text-2xl font-bold text-stone-800"><ksh:credits amount="${stats.credits.inPeriod}"/></div>
            <div class="text-xs text-stone-500 mt-1">Revenue</div>
            <div class="text-xs text-stone-400 mt-0.5"><ksh:credits amount="${stats.credits.all}"/> all-time</div>
        </div>

        <g:render template="/universal/components/statCard"
                  model="[value: stats.catalog.students, label: 'Students', hint: stats.catalog.courses + ' courses']"/>

        <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-4">
            <div class="text-2xl font-bold ${stats.messages.awaiting > 0 ? 'text-rose-600' : 'text-stone-800'}">${stats.messages.awaiting}</div>
            <div class="text-xs text-stone-500 mt-1">Messages need reply</div>
        </div>
    </div>

    <!-- Trend chart -->
    <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-5 mb-4">
        <h3 class="text-sm font-semibold text-stone-700 mb-3">
            Enrollments over time <span class="text-stone-400 font-normal">· ${stats.periodLabel}</span>
        </h3>
        <ksh:trendBars data="${stats.trend}"/>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        <!-- Enrollment health -->
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-5">
            <h3 class="text-sm font-semibold text-stone-700 mb-3">Enrollment health</h3>
            <div class="space-y-2 text-sm">
                <div class="flex justify-between"><span class="text-stone-500">Active</span><span class="font-medium text-stone-800">${stats.enrollments.active}</span></div>
                <div class="flex justify-between"><span class="text-stone-500">Completed</span><span class="font-medium text-stone-800">${stats.enrollments.completed}</span></div>
                <div class="flex justify-between"><span class="text-stone-500">Avg. progress</span><span class="font-medium text-stone-800">${stats.enrollments.avgProgress}%</span></div>
            </div>
            <div class="mt-4">
                <g:render template="/universal/components/progressBar"
                          model="[progress: stats.enrollments.completionRate, label: 'Completion rate']"/>
            </div>
        </div>

        <!-- Top courses -->
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-5">
            <h3 class="text-sm font-semibold text-stone-700 mb-3">Top courses</h3>
            <g:if test="${stats.topCourses}">
                <div class="space-y-2">
                    <g:each in="${stats.topCourses}" var="tc">
                        <div class="flex items-center justify-between text-sm">
                            <span class="text-stone-700 truncate flex-1">${tc.name}</span>
                            <span class="text-stone-400 ml-2 whitespace-nowrap">${tc.enrollments} · <ksh:credits amount="${tc.credits}"/></span>
                        </div>
                    </g:each>
                </div>
            </g:if>
            <g:else>
                <p class="text-sm text-stone-400">No enrollments yet.</p>
            </g:else>
        </div>
    </div>

    <!-- Engagement + needs-reply -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-5">
            <h3 class="text-sm font-semibold text-stone-700 mb-3">Engagement</h3>
            <div class="grid grid-cols-3 gap-3 text-center">
                <div><div class="text-xl font-bold text-stone-800">${stats.engagement.badges}</div><div class="text-xs text-stone-500">Badges</div></div>
                <div><div class="text-xl font-bold text-stone-800">${stats.engagement.reviews}</div><div class="text-xs text-stone-500">Reviews</div></div>
                <div><div class="text-xl font-bold text-stone-800">${stats.engagement.avgRating}</div><div class="text-xs text-stone-500">Avg rating</div></div>
            </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-5">
            <h3 class="text-sm font-semibold text-stone-700 mb-3">Needs a reply</h3>
            <g:if test="${stats.messages.awaitingList}">
                <div class="space-y-1">
                    <g:each in="${stats.messages.awaitingList}" var="conv">
                        <button hx-get="/universal/showView"
                                hx-vals='{"template": "messaging/channel", "data[view]": "service:messageService:channelView", "data[user]": "currentUser", "channelId": "${conv.id}"}'
                                hx-target="#content"
                                hx-swap="innerHTML"
                                class="w-full text-left px-3 py-2 rounded-lg hover:bg-stone-50 flex items-center justify-between min-h-[44px]">
                            <span class="text-sm text-stone-700">${conv.student}</span>
                            <span class="text-xs text-rose-600">Reply →</span>
                        </button>
                    </g:each>
                </div>
            </g:if>
            <g:else>
                <p class="text-sm text-stone-400">All caught up 🎉</p>
            </g:else>
        </div>
    </div>

</g:if>
<g:else>
    <g:render template="/universal/components/emptyState" model="[message: 'No dashboard data available']"/>
</g:else>
