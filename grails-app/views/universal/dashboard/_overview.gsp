<div class="bg-gradient-to-r from-rose-500 to-rose-700 rounded-xl p-6 mb-6 text-white">
    <h2 class="text-2xl font-bold">Welcome back, ${stats?.name ?: 'learner'}! 🇰🇷</h2>
    <p class="text-rose-50 text-sm mt-1">Pick up where you left off and keep your streak going.</p>
</div>

<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
    <g:render template="/universal/components/statCard" model="[value: stats?.enrollments?.total ?: 0, label: 'My courses']"/>
    <g:render template="/universal/components/statCard" model="[value: stats?.enrollments?.completed ?: 0, label: 'Completed']"/>
    <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-4">
        <div class="text-2xl font-bold text-stone-800">${stats?.enrollments?.avgProgress ?: 0}%</div>
        <div class="text-xs text-stone-500 mt-1">Avg. progress</div>
    </div>
    <g:render template="/universal/components/statCard" model="[value: stats?.badges ?: 0, label: 'Badges earned']"/>
</div>

<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
    <button hx-get="/universal/showView"
            hx-vals='{"template": "courses/myCourses", "data[user]": "currentUser", "data[enrollments]": "filter:CourseEnrollment:user.id=currentUserId"}'
            hx-target="#content"
            hx-swap="innerHTML"
            class="bg-white rounded-xl shadow-sm border border-stone-200 p-5 text-left hover:border-rose-300 transition-colors min-h-[44px]">
        <div class="font-semibold text-stone-800">Continue learning →</div>
        <div class="text-sm text-stone-500 mt-1">Jump back into your courses</div>
    </button>
    <button hx-get="/messages/myThread"
            hx-target="#content"
            hx-swap="innerHTML"
            class="bg-white rounded-xl shadow-sm border border-stone-200 p-5 text-left hover:border-rose-300 transition-colors min-h-[44px]">
        <div class="font-semibold text-stone-800">Message the school →</div>
        <div class="text-sm text-stone-500 mt-1">${stats?.messages ?: 0} messages in your thread</div>
    </button>
</div>
