<div class="flex items-center justify-between mb-4">
    <h3 class="text-lg font-semibold text-stone-800">Users</h3>
    <button hx-get="/universal/showView"
            hx-vals='{"template": "admin/userForm", "data[user]": "currentUser"}'
            hx-target="#settings-content"
            hx-swap="innerHTML"
            class="px-4 py-2 bg-rose-700 hover:bg-rose-800 text-white rounded-lg text-sm font-medium transition-colors min-h-[44px]">
        + New User
    </button>
</div>

<g:if test="${users}">
    <div class="space-y-3">
        <g:each in="${users}" var="u">
            <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-4">
                <div class="flex gap-4 items-center">
                    <div class="w-12 h-12 rounded-full bg-rose-100 flex items-center justify-center flex-shrink-0">
                        <span class="text-sm font-bold text-rose-700">${(u.name ?: u.username)?.charAt(0)?.toUpperCase()}</span>
                    </div>
                    <div class="flex-1 min-w-0">
                        <h3 class="font-semibold text-stone-800 text-sm">${u.name ?: u.username}</h3>
                        <p class="text-xs text-stone-500">${u.email ?: 'No email'} &middot; ${u.username}</p>
                        <div class="flex gap-2 mt-1 flex-wrap">
                            <g:each in="${u.authorities}" var="role">
                                <span class="text-xs px-2 py-0.5 rounded-full
                                    ${role.authority == 'ROLE_ADMIN' ? 'bg-red-100 text-red-700' :
                                      role.authority == 'ROLE_TEACHER' ? 'bg-blue-100 text-blue-700' :
                                      'bg-stone-100 text-stone-600'}">
                                    ${role.authority == 'ROLE_ADMIN' ? 'Admin' :
                                      role.authority == 'ROLE_TEACHER' ? 'Creator' :
                                      role.authority == 'ROLE_USER' ? 'Learner' : role.authority}
                                </span>
                            </g:each>
                            <g:if test="${!u.enabled}">
                                <span class="text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-700">Disabled</span>
                            </g:if>
                        </div>
                    </div>
                    <div class="flex gap-2 flex-shrink-0">
                        <button hx-get="/universal/showView"
                                hx-vals='{"template": "admin/userEdit", "data[editUser]": "get:User:userId", "data[user]": "currentUser", "userId": "${u.id}"}'
                                hx-target="#settings-content"
                                hx-swap="innerHTML"
                                class="px-3 py-2 bg-stone-100 hover:bg-stone-200 text-stone-700 rounded-lg text-xs font-medium transition-colors min-h-[44px]">
                            Edit
                        </button>
                        <button hx-post="/user/delete/${u.id}"
                                hx-target="#settings-content"
                                hx-swap="innerHTML"
                                hx-confirm="Delete ${u.name ?: u.username}? This cannot be undone."
                                class="px-3 py-2 bg-red-50 hover:bg-red-100 text-red-700 rounded-lg text-xs font-medium transition-colors min-h-[44px]">
                            Delete
                        </button>
                    </div>
                </div>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <g:render template="/universal/components/emptyState" model="[message: 'No users yet. Tap + New User to create one.']"/>
</g:else>
