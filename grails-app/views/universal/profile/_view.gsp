<%
    def badges = user ? ksh.UserBadge.findAllByUser(user) : []
    def enrollmentCount = user ? ksh.CourseEnrollment.countByUser(user) : 0
    def completedCount = user ? ksh.CourseEnrollment.countByUserAndCompletedAtIsNotNull(user) : 0
%>

<div class="bg-white rounded-xl shadow-sm border border-stone-200 p-6 mb-6">
    <!-- User Info -->
    <div class="flex items-center gap-4 mb-6">
        <div class="w-16 h-16 rounded-full bg-rose-100 flex items-center justify-center flex-shrink-0">
            <g:if test="${user.avatar}">
                <img src="${user.avatar}" alt="${user.name}" class="w-16 h-16 rounded-full object-cover"/>
            </g:if>
            <g:else>
                <span class="text-2xl font-bold text-rose-700">${(user.name ?: user.username)?.charAt(0)?.toUpperCase()}</span>
            </g:else>
        </div>
        <div>
            <h2 class="text-xl font-bold text-stone-800">${user.name ?: user.username}</h2>
            <p class="text-sm text-stone-500 capitalize">${user.roleType?.replace('_', ' ')}</p>
        </div>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-3 gap-4 text-center">
        <div class="p-3 rounded-lg bg-stone-50">
            <div class="text-xl font-bold text-stone-800">${user.kCredits}</div>
            <div class="text-xs text-stone-500">K-Credits</div>
        </div>
        <div class="p-3 rounded-lg bg-stone-50">
            <div class="text-xl font-bold text-stone-800">${enrollmentCount}</div>
            <div class="text-xs text-stone-500">Courses</div>
        </div>
        <div class="p-3 rounded-lg bg-stone-50">
            <div class="text-xl font-bold text-stone-800">${user.points}</div>
            <div class="text-xs text-stone-500">Points</div>
        </div>
    </div>
</div>

<!-- Badges -->
<div class="bg-white rounded-xl shadow-sm border border-stone-200 p-6 mb-6">
    <h3 class="text-lg font-semibold text-stone-800 mb-4">Badges</h3>
    <g:if test="${badges}">
        <div class="grid grid-cols-4 md:grid-cols-6 gap-4">
            <g:each in="${badges}" var="ub">
                <g:render template="/universal/components/badge" model="[badge: ub.badge]"/>
            </g:each>
        </div>
    </g:if>
    <g:else>
        <p class="text-sm text-stone-400">No badges earned yet. Complete courses to earn badges!</p>
    </g:else>
</div>

<!-- Wall Posts -->
<div class="bg-white rounded-xl shadow-sm border border-stone-200 p-6">
    <h3 class="text-lg font-semibold text-stone-800 mb-4">Wall</h3>

    <div id="wall-posts"
         hx-get="/universal/showView"
         hx-vals='{"template": "profile/wall", "data[user]": "currentUser"}'
         hx-trigger="load"
         hx-swap="innerHTML">
    </div>
</div>
