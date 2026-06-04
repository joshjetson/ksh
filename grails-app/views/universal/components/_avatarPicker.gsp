<%-- Fixed-set preset avatar picker.
     Renders 8 preset avatars; clicking one fills a hidden #presetAvatar input.
     Used in profile/_edit.gsp when AppConfig.profileUploadEnabled = false. --%>
<% def presets = ksh.AvatarController.presetSlugs %>
<% def currentSlug = user?.avatar?.startsWith('/avatar/preset/') ? user.avatar.substring('/avatar/preset/'.length()) : null %>

<div>
    <label class="block text-sm font-medium text-stone-700 mb-2">Choose a profile picture</label>
    <input type="hidden" id="presetAvatar" name="presetAvatar" value="${currentSlug ?: ''}"/>
    <div class="grid grid-cols-4 sm:grid-cols-8 gap-3" id="avatar-picker">
        <g:each in="${presets}" var="slug">
            <button type="button"
                    data-slug="${slug}"
                    onclick="selectPresetAvatar(this)"
                    class="avatar-choice rounded-full overflow-hidden border-2 transition-all min-h-[44px] min-w-[44px] ${slug == currentSlug ? 'border-rose-700 ring-2 ring-rose-300' : 'border-transparent hover:border-stone-300'}">
                <img src="/avatar/preset/${slug}" alt="${slug}" class="w-full h-full block"/>
            </button>
        </g:each>
    </div>
    <p class="text-xs text-stone-400 mt-2">Profile picture uploads are disabled. Pick from this set.</p>
</div>

<script>
    function selectPresetAvatar(btn) {
        document.querySelectorAll('#avatar-picker .avatar-choice').forEach(function(b) {
            b.classList.remove('border-rose-700', 'ring-2', 'ring-rose-300');
            b.classList.add('border-transparent');
        });
        btn.classList.remove('border-transparent');
        btn.classList.add('border-rose-700', 'ring-2', 'ring-rose-300');
        document.getElementById('presetAvatar').value = btn.dataset.slug;
    }
</script>
