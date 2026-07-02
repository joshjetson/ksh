<%-- Settings shell. Privacy tab for every signed-in user; User Management +
     Customization stay admin-only. The Privacy tab is the default; admins get the
     extra tabs alongside it. --%>
<h2 class="text-xl font-bold text-stone-800 mb-4">Settings</h2>

<div class="flex gap-2 mb-6 overflow-x-auto" id="settings-nav">
    <button class="settings-tab px-4 py-2 text-sm font-medium rounded-full min-h-[44px] bg-stone-800 text-white"
            hx-get="/universal/showView"
            hx-vals='{"template": "settings/privacy", "data[user]": "currentUser", "data[settings]": "filter:UserSetting:user.id=currentUserId", "data[blocks]": "filter:UserBlock:owner.id=currentUserId"}'
            hx-target="#settings-content"
            hx-swap="innerHTML"
            onclick="setActiveSettingsTab(this)">
        Privacy
    </button>
    <sec:ifAnyGranted roles="ROLE_ADMIN">
        <button class="settings-tab px-4 py-2 text-sm font-medium rounded-full min-h-[44px] bg-stone-100 text-stone-600"
                hx-get="/universal/showView"
                hx-vals='{"template": "admin/userList", "data[users]": "list:User"}'
                hx-target="#settings-content"
                hx-swap="innerHTML"
                onclick="setActiveSettingsTab(this)">
            User Management
        </button>
        <button class="settings-tab px-4 py-2 text-sm font-medium rounded-full min-h-[44px] bg-stone-100 text-stone-600"
                hx-get="/universal/showView"
                hx-vals='{"template": "settings/customization", "data[configs]": "list:AppConfig"}'
                hx-target="#settings-content"
                hx-swap="innerHTML"
                onclick="setActiveSettingsTab(this)">
            Customization
        </button>
    </sec:ifAnyGranted>
</div>

<div id="settings-content"
     hx-get="/universal/showView"
     hx-vals='{"template": "settings/privacy", "data[user]": "currentUser", "data[settings]": "filter:UserSetting:user.id=currentUserId", "data[blocks]": "filter:UserBlock:owner.id=currentUserId"}'
     hx-trigger="load"
     hx-swap="innerHTML">
</div>

<script>
    function setActiveSettingsTab(el) {
        document.querySelectorAll('#settings-nav .settings-tab').forEach(function(btn) {
            btn.classList.remove('bg-stone-800', 'text-white');
            btn.classList.add('bg-stone-100', 'text-stone-600');
        });
        el.classList.remove('bg-stone-100', 'text-stone-600');
        el.classList.add('bg-stone-800', 'text-white');
    }
</script>
