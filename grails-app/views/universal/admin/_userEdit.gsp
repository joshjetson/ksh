<div class="flex items-center gap-3 mb-6">
    <button hx-get="/universal/showView"
            hx-vals='{"template": "admin/userList", "data[users]": "list:User"}'
            hx-target="#settings-content"
            hx-swap="innerHTML"
            class="text-stone-500 hover:text-stone-700 min-h-[44px] py-2">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
    </button>
    <h3 class="text-lg font-semibold text-stone-800">Edit User — ${editUser.name ?: editUser.username}</h3>
</div>

<div class="bg-white rounded-xl shadow-sm border border-stone-200 p-6">
    <form hx-post="/user/update/${editUser.id}"
          hx-target="#settings-content"
          hx-swap="innerHTML"
          class="space-y-4">

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <g:render template="/universal/components/input" model="[name: 'firstName', label: 'First Name', value: editUser.firstName, required: true]"/>
            <g:render template="/universal/components/input" model="[name: 'lastName', label: 'Last Name', value: editUser.lastName, required: true]"/>
        </div>

        <g:render template="/universal/components/input" model="[name: 'email', label: 'Email Address', type: 'email', value: editUser.email, required: true]"/>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <g:render template="/universal/components/input" model="[name: 'username', label: 'Username', value: editUser.username, required: true]"/>
            <div>
                <label for="password" class="block text-sm font-medium text-stone-700 mb-1">Password</label>
                <input type="password" id="password" name="password" placeholder="Leave blank to keep current"
                       class="block w-full px-4 py-3 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-transparent text-base"/>
            </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <g:render template="/universal/components/input" model="[name: 'title', label: 'Title', value: editUser.title]"/>
            <g:render template="/universal/components/input" model="[name: 'country', label: 'Country', value: editUser.country]"/>
        </div>

        <g:render template="/universal/components/input" model="[name: 'dateOfBirth', label: 'Date of Birth', type: 'date', value: editUser.dateOfBirth?.format('yyyy-MM-dd'), required: true]"/>

        <!-- Roles -->
        <% def currentRoles = editUser.authorities*.authority %>
        <div>
            <label class="block text-sm font-medium text-stone-700 mb-2">Roles *</label>
            <div class="space-y-2">
                <label class="flex items-center gap-2 min-h-[44px]">
                    <input type="checkbox" checked disabled class="rounded border-stone-300 text-rose-700"/>
                    <span class="text-sm text-stone-700">Learner</span>
                    <span class="text-xs text-stone-400">(always assigned)</span>
                </label>
                <label class="flex items-center gap-2 min-h-[44px]">
                    <input type="checkbox" name="roles" value="ROLE_TEACHER"
                           ${currentRoles.contains('ROLE_TEACHER') ? 'checked' : ''}
                           class="rounded border-stone-300 text-rose-700"/>
                    <span class="text-sm text-stone-700">Creator</span>
                    <span class="text-xs text-stone-400">(can create courses)</span>
                </label>
                <label class="flex items-center gap-2 min-h-[44px]">
                    <input type="checkbox" name="roles" value="ROLE_ADMIN"
                           ${currentRoles.contains('ROLE_ADMIN') ? 'checked' : ''}
                           class="rounded border-stone-300 text-rose-700"/>
                    <span class="text-sm text-stone-700">Admin</span>
                    <span class="text-xs text-stone-400">(full access)</span>
                </label>
            </div>
        </div>

        <div class="pt-2">
            <g:render template="/universal/components/button" model="[text: 'Save Changes', type: 'submit']"/>
        </div>
    </form>
</div>
