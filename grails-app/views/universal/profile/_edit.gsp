<div class="flex items-center gap-3 mb-6">
    <button hx-get="/universal/showView"
            hx-vals='{"template": "profile/view", "data[user]": "currentUser", "data[badges]": "filter:UserBadge:user.id=currentUserId", "data[enrollmentCount]": "filterCount:CourseEnrollment:user.id=currentUserId", "data[config]": "get:AppConfig:configId", "configId": "1"}'
            hx-target="#content"
            hx-swap="innerHTML"
            class="text-stone-500 hover:text-stone-700 min-h-[44px] py-2">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
    </button>
    <h2 class="text-xl font-bold text-stone-800">Edit Profile</h2>
</div>

<div class="bg-white rounded-xl shadow-sm border border-stone-200 p-6">
    <form hx-post="/user/updateProfile"
          hx-target="#content"
          hx-swap="innerHTML"
          hx-encoding="${config?.profileUploadEnabled ? 'multipart/form-data' : ''}"
          class="space-y-4">

        <!-- Avatar -->
        <div class="flex items-center gap-4 mb-2">
            <div class="w-20 h-20 rounded-full bg-rose-100 flex items-center justify-center flex-shrink-0 overflow-hidden">
                <g:if test="${user.avatar}">
                    <img src="${user.avatar}" alt="${user.name}" class="w-20 h-20 rounded-full object-cover"/>
                </g:if>
                <g:else>
                    <span class="text-3xl font-bold text-rose-700">${(user.name ?: user.username)?.charAt(0)?.toUpperCase()}</span>
                </g:else>
            </div>
            <div>
                <g:if test="${config?.profileUploadEnabled}">
                    <label class="block text-sm font-medium text-stone-700 mb-1">Profile Picture</label>
                    <input type="file" name="avatarFile" accept="image/*"
                           class="text-sm text-stone-500 file:mr-2 file:py-2 file:px-3 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-stone-100 file:text-stone-700 hover:file:bg-stone-200"/>
                    <g:if test="${user.avatar}">
                        <label class="flex items-center gap-2 mt-2 min-h-[44px]">
                            <input type="checkbox" name="removeAvatar" value="true" class="rounded border-stone-300 text-rose-700"/>
                            <span class="text-xs text-stone-500">Remove current picture</span>
                        </label>
                    </g:if>
                </g:if>
                <g:else>
                    <p class="text-xs text-stone-400">Profile picture uploads are currently disabled</p>
                </g:else>
            </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <g:render template="/universal/components/input" model="[name: 'firstName', label: 'First Name', value: user.firstName, required: true]"/>
            <g:render template="/universal/components/input" model="[name: 'lastName', label: 'Last Name', value: user.lastName, required: true]"/>
        </div>

        <g:render template="/universal/components/input" model="[name: 'title', label: 'Title', value: user.title, placeholder: 'e.g. Korean Language Student']"/>

        <div class="pt-2">
            <g:render template="/universal/components/button" model="[text: 'Save Profile', type: 'submit']"/>
        </div>
    </form>
</div>
