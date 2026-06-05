<%
    // Slice-picking only: badges not already attached to this course.
    def attachedIds = courseRewards*.badge*.id
    def available = (allBadges ?: []).findAll { !attachedIds.contains(it.id) }
%>
<p class="text-sm font-medium text-stone-700 mb-1">Rewards on completion</p>
<p class="text-xs text-stone-400 mb-3">Badges &amp; avatars awarded automatically when a learner finishes this course.</p>

<g:if test="${courseRewards}">
    <div class="flex flex-wrap gap-2 mb-3">
        <g:each in="${courseRewards}" var="cr">
            <span class="inline-flex items-center gap-1 bg-amber-50 border border-amber-200 rounded-full pl-3 pr-1 py-1 text-xs text-amber-800">
                ${cr.badge?.name}
                <button type="button"
                        hx-post="/universal/delete/${cr.id}?domainName=CourseReward"
                        hx-vals='{"template": "courses/courseRewards", "data[course]": "get:Course:forCourse", "data[courseRewards]": "filter:CourseReward:course.id=forCourse", "data[allBadges]": "list:Badge", "forCourse": "${course.id}"}'
                        hx-target="#course-rewards" hx-swap="innerHTML"
                        class="w-5 h-5 flex items-center justify-center rounded-full hover:bg-amber-200 text-amber-600"
                        aria-label="Remove ${cr.badge?.name}">&times;</button>
            </span>
        </g:each>
    </div>
</g:if>
<g:else>
    <p class="text-xs text-stone-400 mb-3">No badges attached yet.</p>
</g:else>

<g:if test="${available}">
    <div class="flex gap-2">
        <select name="badge.id" id="cr-badge-select"
                class="flex-1 px-3 py-2 border border-stone-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-rose-500">
            <option value="">Choose a badge…</option>
            <g:each in="${available}" var="b">
                <option value="${b.id}">${b.name}</option>
            </g:each>
        </select>
        <input type="hidden" name="course.id" id="cr-course-id" value="${course.id}"/>
        <button type="button"
                hx-post="/universal/save?domainName=CourseReward"
                hx-vals='{"template": "courses/courseRewards", "data[course]": "get:Course:forCourse", "data[courseRewards]": "filter:CourseReward:course.id=forCourse", "data[allBadges]": "list:Badge", "forCourse": "${course.id}"}'
                hx-include="#cr-badge-select, #cr-course-id"
                hx-target="#course-rewards" hx-swap="innerHTML"
                class="px-4 py-2 bg-stone-200 hover:bg-stone-300 text-stone-800 rounded-lg text-sm font-medium min-h-[44px]">Attach</button>
    </div>
</g:if>
<g:else>
    <p class="text-xs text-stone-400">All badges attached. Create more in the Rewards tab.</p>
</g:else>
