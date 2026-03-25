<div class="flex items-center justify-between mb-6">
    <div class="flex items-center gap-3">
        <button hx-get="/universal/showView"
                hx-vals='{"template": "profile/view", "data[user]": "currentUser", "data[badges]": "filter:UserBadge:user.id=currentUserId", "data[enrollmentCount]": "filterCount:CourseEnrollment:user.id=currentUserId"}'
                hx-target="#content"
                hx-swap="innerHTML"
                class="text-stone-500 hover:text-stone-700 min-h-[44px] py-2">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
        </button>
        <h2 class="text-xl font-bold text-stone-800">Manage Users</h2>
    </div>
    <button hx-get="/universal/showView"
            hx-vals='{"template": "admin/userForm", "data[user]": "currentUser"}'
            hx-target="#content"
            hx-swap="innerHTML"
            class="px-4 py-3 bg-rose-700 hover:bg-rose-800 text-white rounded-lg text-sm font-medium transition-colors min-h-[44px]">
        + New User
    </button>
</div>

<div id="user-list"
     hx-get="/universal/showView"
     hx-vals='{"template": "admin/userList", "data[users]": "list:User"}'
     hx-trigger="load"
     hx-swap="innerHTML">
</div>
