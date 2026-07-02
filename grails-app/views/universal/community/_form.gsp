<%-- Share / edit a post or kudos. Model: item (CommunityPost, optional — managers edit),
     user. Anyone may post; everyone chooses visibility (public / staff / self). author +
     anonymous handled server-side. Lands on My Wall after saving so the author sees it. --%>
<%
    def editing = item?.id != null
    def action  = editing ? "/universal/update/${item.id}?domainName=CommunityPost" : "/universal/save?domainName=CommunityPost"
    def back    = '{"template": "community/myWall", "data[walls]": "service:communityService:myWall", "data[user]": "currentUser"}'
    String curKind = item?.kind ?: 'post'
    String curVis  = item?.visibility ?: 'public'
%>
<div class="max-w-lg mx-auto">
  <button class="text-stone-400 hover:text-rose-700 text-sm mb-4" hx-get="/universal/showView" hx-vals='${back}' hx-target="#content" hx-swap="innerHTML">&larr; Back</button>
  <div class="card p-6">
    <h2 class="text-xl font-bold text-stone-800 mb-5">${editing ? 'Edit your post' : 'Share with our community'}</h2>
    <form hx-post="${action}" hx-vals='${back}' hx-target="#content" hx-swap="innerHTML" class="space-y-5">

      <div>
        <span class="block text-sm font-medium text-stone-700 mb-2">What would you like to share?</span>
        <div class="grid grid-cols-2 gap-3">
          <label class="cursor-pointer">
            <input type="radio" name="kind" value="post" class="peer sr-only" ${curKind == 'post' ? 'checked' : ''}/>
            <div class="rounded-2xl border-2 border-cream-300 p-4 text-center peer-checked:border-rose-500 peer-checked:bg-rose-50 transition-colors">
              <div class="text-2xl" aria-hidden="true">💬</div>
              <div class="mt-1 font-semibold text-stone-800 text-sm">Community post</div>
            </div>
          </label>
          <label class="cursor-pointer">
            <input type="radio" name="kind" value="kudos" class="peer sr-only" ${curKind == 'kudos' ? 'checked' : ''}/>
            <div class="rounded-2xl border-2 border-cream-300 p-4 text-center peer-checked:border-gold-500 peer-checked:bg-gold-500/5 transition-colors">
              <div class="text-2xl" aria-hidden="true">🎉</div>
              <div class="mt-1 font-semibold text-stone-800 text-sm">Kudos</div>
            </div>
          </label>
        </div>
      </div>

      <g:render template="/universal/components/input" model="[name: 'title', label: 'Title', required: true, value: item?.title]"/>
      <g:render template="/universal/components/textarea" model="[name: 'body', label: 'Details', rows: 4, required: true, value: item?.body]"/>

      <div>
        <span class="block text-sm font-medium text-stone-700 mb-2">Who can see this?</span>
        <div class="space-y-2">
          <label class="cursor-pointer block">
            <input type="radio" name="visibility" value="public" class="peer sr-only" ${curVis == 'public' ? 'checked' : ''}/>
            <div class="flex items-center gap-3 rounded-xl border-2 border-cream-300 p-3 peer-checked:border-rose-500 peer-checked:bg-rose-50 transition-colors">
              <span class="text-xl" aria-hidden="true">🌏</span>
              <div><div class="font-semibold text-stone-800 text-sm">Whole community</div><div class="text-xs text-stone-400">Shows on the community wall</div></div>
            </div>
          </label>
          <label class="cursor-pointer block">
            <input type="radio" name="visibility" value="staff" class="peer sr-only" ${curVis == 'staff' ? 'checked' : ''}/>
            <div class="flex items-center gap-3 rounded-xl border-2 border-cream-300 p-3 peer-checked:border-rose-500 peer-checked:bg-rose-50 transition-colors">
              <span class="text-xl" aria-hidden="true">🛡️</span>
              <div><div class="font-semibold text-stone-800 text-sm">Staff only</div><div class="text-xs text-stone-400">Just our teachers &amp; admin</div></div>
            </div>
          </label>
          <label class="cursor-pointer block">
            <input type="radio" name="visibility" value="self" class="peer sr-only" ${curVis == 'self' ? 'checked' : ''}/>
            <div class="flex items-center gap-3 rounded-xl border-2 border-cream-300 p-3 peer-checked:border-rose-500 peer-checked:bg-rose-50 transition-colors">
              <span class="text-xl" aria-hidden="true">🔒</span>
              <div><div class="font-semibold text-stone-800 text-sm">Just me</div><div class="text-xs text-stone-400">Private to your own wall</div></div>
            </div>
          </label>
        </div>
      </div>

      <label class="flex items-center gap-2 text-sm text-stone-700">
        <input type="hidden" name="_anonymous"/>
        <input type="checkbox" name="anonymous" value="true" ${item?.anonymous ? 'checked' : ''} class="h-4 w-4 rounded border-cream-300 text-rose-600 focus:ring-rose-500"/>
        Post without my name (anonymous)
      </label>

      <g:render template="/universal/components/button" model="[text: editing ? 'Save' : 'Share', type: 'submit', variant: 'primary', full: true]"/>
    </form>
  </div>
</div>
