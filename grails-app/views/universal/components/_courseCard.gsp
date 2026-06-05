<div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden cursor-pointer hover:shadow-md transition-shadow"
     hx-get="/universal/showView"
     hx-vals='{"template": "courses/preview", "data[course]": "get:Course:courseId", "data[user]": "currentUser", "data[enrolled]": "exists:CourseEnrollment:user.id=currentUserId,course.id=courseId", "courseId": "${course.id}"}'
     hx-target="#content"
     hx-swap="innerHTML">
    <div class="aspect-video bg-stone-100 flex items-center justify-center">
        <ksh:courseImage course="${course}" imgClass="w-full h-full object-cover" emojiClass="text-4xl"/>
    </div>
    <div class="p-4">
        <h3 class="font-semibold text-stone-800 text-base mb-1 line-clamp-1">${course.shortTitle}</h3>
        <p class="text-sm text-stone-500 mb-3 line-clamp-2">${course.shortDescription ?: ''}</p>
        <div class="flex items-center justify-between">
            <span class="text-sm font-medium ${course.costKCredits > 0 ? 'text-rose-700' : 'text-green-600'}">
                ${course.costKCredits > 0 ? course.costKCredits + ' K-Credits' : 'Free'}
            </span>
            <g:if test="${course.pointReward > 0}">
                <span class="text-xs text-stone-400">+${course.pointReward} pts</span>
            </g:if>
        </div>
    </div>
</div>
