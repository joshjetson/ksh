<div class="flex items-center justify-between mb-6">
    <h2 class="text-xl font-bold text-stone-800">My Lessons</h2>
    <button hx-get="/universal/showView"
            hx-vals='{"template": "courses/courseForm", "data[user]": "currentUser"}'
            hx-target="#content"
            hx-swap="innerHTML"
            class="px-4 py-3 bg-rose-700 hover:bg-rose-800 text-white rounded-lg text-sm font-medium transition-colors min-h-[44px]">
        + New Course
    </button>
</div>

<g:if test="${myCourses}">
    <div class="space-y-4">
        <g:each in="${myCourses}" var="course">
            <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-4 flex gap-4 items-center">
                <!-- Thumbnail -->
                <div class="w-20 h-20 bg-stone-100 rounded-lg flex-shrink-0 flex items-center justify-center overflow-hidden">
                    <g:if test="${course.thumbnailSmall}">
                        <img src="${course.thumbnailSmall}" alt="${course.shortTitle}" class="w-full h-full object-cover"/>
                    </g:if>
                    <g:else>
                        <span class="text-2xl">&#128218;</span>
                    </g:else>
                </div>

                <!-- Info -->
                <div class="flex-1 min-w-0">
                    <h3 class="font-semibold text-stone-800 text-base line-clamp-1">${course.shortTitle}</h3>
                    <p class="text-sm text-stone-500 line-clamp-1">${course.shortDescription ?: ''}</p>
                    <div class="flex items-center gap-3 mt-1 flex-wrap">
                        <span class="text-xs font-medium ${course.costKCredits > 0 ? 'text-rose-700' : 'text-green-600'}">
                            ${course.costKCredits > 0 ? course.costKCredits + ' K-Credits' : 'Free'}
                        </span>
                        <g:if test="${course.scormFileName}">
                            <span class="text-xs text-green-600 font-medium">SCORM</span>
                        </g:if>
                        <g:else>
                            <span class="text-xs text-amber-600">No SCORM</span>
                        </g:else>
                    </div>
                </div>

                <!-- Edit button -->
                <button hx-get="/universal/showView"
                        hx-vals='{"template": "courses/courseForm", "data[course]": "get:Course:courseId", "data[user]": "currentUser", "courseId": "${course.id}"}'
                        hx-target="#content"
                        hx-swap="innerHTML"
                        class="px-3 py-2 bg-stone-200 hover:bg-stone-300 text-stone-700 rounded-lg text-sm font-medium transition-colors min-h-[44px]">
                    Edit
                </button>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <g:render template="/universal/components/emptyState" model="[message: 'No courses created yet. Tap + New Course to get started.']"/>
</g:else>
