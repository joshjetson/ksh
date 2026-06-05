<!-- Create-tab sub-nav -->
<div class="flex gap-2 mb-6">
    <button hx-get="/universal/showView"
            hx-vals='{"template": "lessons/manage", "data[user]": "currentUser", "data[myCourses]": "filter:Course:creator.id=currentUserId"}'
            hx-target="#content" hx-swap="innerHTML"
            class="px-4 py-2 rounded-lg text-sm font-medium bg-stone-100 text-stone-600 hover:bg-stone-200 min-h-[44px]">Lessons</button>
    <button class="px-4 py-2 rounded-lg text-sm font-medium bg-rose-700 text-white min-h-[44px]">Rewards</button>
</div>

<div class="flex items-center justify-between mb-6 gap-3">
    <h2 class="text-xl font-bold text-stone-800">Badges &amp; Rewards</h2>
    <button hx-get="/universal/showView"
            hx-vals='{"template": "rewards/form"}'
            hx-target="#content" hx-swap="innerHTML"
            class="px-4 py-3 bg-rose-700 hover:bg-rose-800 text-white rounded-lg text-sm font-medium min-h-[44px]">
        + New reward
    </button>
</div>

<g:if test="${badges}">
    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        <g:each in="${badges}" var="b">
            <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-4 text-center">
                <div class="w-16 h-16 mx-auto rounded-full bg-stone-100 flex items-center justify-center overflow-hidden mb-2">
                    <g:if test="${b.hasImage()}">
                        <img src="/badge/image/${b.id}" alt="${b.name}" class="w-full h-full object-cover"/>
                    </g:if>
                    <g:elseif test="${b.icon}">
                        <span class="text-2xl">${b.icon}</span>
                    </g:elseif>
                    <g:else>
                        <span class="text-2xl">&#127941;</span>
                    </g:else>
                </div>
                <div class="font-medium text-stone-800 text-sm truncate">${b.name}</div>
                <g:if test="${b.kind?.name() == 'AVATAR'}">
                    <span class="inline-block text-xs px-2 py-0.5 rounded-full bg-sky-100 text-sky-800 mt-1">Avatar</span>
                </g:if>
                <g:else>
                    <span class="inline-block text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 mt-1">Badge</span>
                </g:else>
                <div class="flex justify-center gap-3 mt-2">
                    <button hx-get="/universal/showView"
                            hx-vals='{"template": "rewards/form", "data[badge]": "get:Badge:badgeId", "badgeId": "${b.id}"}'
                            hx-target="#content" hx-swap="innerHTML"
                            class="text-xs text-stone-500 hover:text-stone-700">Edit</button>
                    <button hx-post="/universal/delete/${b.id}?domainName=Badge"
                            hx-vals='{"template": "rewards/manage", "data[badges]": "list:Badge"}'
                            hx-target="#content" hx-swap="innerHTML"
                            hx-confirm="Delete ${b.name}?"
                            class="text-xs text-red-500 hover:text-red-700">Delete</button>
                </div>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <g:render template="/universal/components/emptyState" model="[message: 'No badges or rewards yet. Create your first one!']"/>
</g:else>
