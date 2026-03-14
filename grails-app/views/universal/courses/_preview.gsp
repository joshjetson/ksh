<!-- Back button -->
<button hx-get="/universal/showView"
        hx-vals='{"template": "courses/browse", "data[courses]": "list:Course", "data[user]": "currentUser"}'
        hx-target="#content"
        hx-swap="innerHTML"
        class="flex items-center gap-2 text-sm text-stone-500 hover:text-stone-700 mb-4 min-h-[44px] py-2">
    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
    </svg>
    Back to courses
</button>

<div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden">
    <!-- Hero image -->
    <div class="aspect-video bg-stone-100 flex items-center justify-center">
        <g:if test="${course.thumbnailLarge}">
            <img src="${course.thumbnailLarge}" alt="${course.shortTitle}" class="w-full h-full object-cover"/>
        </g:if>
        <g:else>
            <span class="text-6xl">&#128218;</span>
        </g:else>
    </div>

    <div class="p-6">
        <!-- Title & Creator -->
        <h1 class="text-2xl font-bold text-stone-800 mb-1">${course.longTitle}</h1>
        <p class="text-sm text-stone-500 mb-4">by ${course.creator?.name ?: course.creator?.username}</p>

        <!-- Tags -->
        <g:if test="${course.tags}">
            <div class="flex flex-wrap gap-2 mb-4">
                <g:each in="${course.tags?.split(',')}" var="tag">
                    <span class="px-2 py-1 bg-stone-100 text-stone-600 rounded-full text-xs">${tag.trim()}</span>
                </g:each>
            </div>
        </g:if>

        <!-- Price & Rewards -->
        <div class="flex items-center gap-4 mb-6 p-4 rounded-lg" style="background-color: #fff8f0;">
            <div>
                <span class="text-lg font-bold ${course.costKCredits > 0 ? 'text-rose-700' : 'text-green-600'}">
                    ${course.costKCredits > 0 ? course.costKCredits + ' K-Credits' : 'Free'}
                </span>
            </div>
            <g:if test="${course.pointReward > 0}">
                <div class="text-sm text-stone-500">+${course.pointReward} points on completion</div>
            </g:if>
            <g:if test="${course.badgeReward}">
                <div class="text-sm text-stone-500">&#127942; Badge reward</div>
            </g:if>
        </div>

        <!-- Enroll button -->
        <g:if test="${user}">
            <g:if test="${enrolled}">
                <g:if test="${course.scormLaunchUrl}">
                    <a href="/scorm/player/${course.id}"
                       class="mb-6 w-full py-3 px-4 rounded-lg text-center text-white font-medium bg-rose-700 hover:bg-rose-800 block text-base min-h-[44px]">
                        Launch Course
                    </a>
                </g:if>
                <g:else>
                    <div class="mb-6 w-full py-3 px-4 rounded-lg text-center text-green-700 font-medium bg-green-50 border border-green-200 text-base">
                        Already enrolled
                    </div>
                </g:else>
            </g:if>
            <g:else>
                <form hx-post="/universal/save?domainName=CourseEnrollment"
                      hx-vals='{"template": "courses/myCourses", "data[user]": "currentUser", "data[enrollments]": "filter:CourseEnrollment:user.id=currentUserId"}'
                      hx-target="#content"
                      hx-swap="innerHTML"
                      class="mb-6">
                    <input type="hidden" name="user.id" value="${user.id}"/>
                    <input type="hidden" name="course.id" value="${course.id}"/>
                    <button type="submit"
                            class="w-full py-3 px-4 rounded-lg text-white font-medium bg-rose-700 hover:bg-rose-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-rose-500 transition-colors text-base">
                        ${course.costKCredits > 0 ? 'Purchase for ' + course.costKCredits + ' K-Credits' : 'Enroll for Free'}
                    </button>
                </form>
            </g:else>
        </g:if>

        <!-- Description -->
        <div class="prose prose-stone max-w-none">
            <h3 class="text-lg font-semibold text-stone-800 mb-2">About this course</h3>
            <p class="text-stone-600 text-sm leading-relaxed">${course.longDescription ?: course.shortDescription ?: 'No description available.'}</p>
        </div>
    </div>
</div>
