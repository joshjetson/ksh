<%
    def enrollments = user ? ksh.CourseEnrollment.findAllByUser(user) : []
%>

<h2 class="text-xl font-bold text-stone-800 mb-4">My Courses</h2>

<g:if test="${enrollments}">
    <div class="space-y-4">
        <g:each in="${enrollments}" var="enrollment">
            <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-4 flex gap-4 items-center cursor-pointer hover:shadow-md transition-shadow"
                 hx-get="/universal/showView"
                 hx-vals='{"template": "courses/preview", "data[course]": "get:Course:courseId", "data[user]": "currentUser", "courseId": "${enrollment.course.id}"}'
                 hx-target="#content"
                 hx-swap="innerHTML">

                <!-- Thumbnail -->
                <div class="w-20 h-20 bg-stone-100 rounded-lg flex-shrink-0 flex items-center justify-center overflow-hidden">
                    <g:if test="${enrollment.course.thumbnailSmall}">
                        <img src="${enrollment.course.thumbnailSmall}" alt="${enrollment.course.shortTitle}" class="w-full h-full object-cover"/>
                    </g:if>
                    <g:else>
                        <span class="text-2xl">&#128218;</span>
                    </g:else>
                </div>

                <!-- Info -->
                <div class="flex-1 min-w-0">
                    <h3 class="font-semibold text-stone-800 text-base line-clamp-1">${enrollment.course.shortTitle}</h3>
                    <g:if test="${enrollment.completedAt}">
                        <span class="text-xs text-green-600 font-medium">Completed</span>
                    </g:if>
                    <g:else>
                        <div class="mt-2">
                            <g:render template="/universal/components/progressBar" model="[progress: enrollment.progress, label: '']"/>
                        </div>
                    </g:else>
                </div>

                <!-- Launch button -->
                <g:if test="${enrollment.course.scormLaunchUrl}">
                    <a href="/scorm/player/${enrollment.course.id}"
                       onclick="event.stopPropagation()"
                       class="px-4 py-2 bg-rose-700 hover:bg-rose-800 text-white rounded-lg text-sm font-medium min-h-[44px] flex items-center flex-shrink-0">
                        Launch
                    </a>
                </g:if>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <g:render template="/universal/components/emptyState" model="[message: 'No enrolled courses yet. Browse courses to get started!']"/>
</g:else>
