<%-- The user's own wall. Model: walls (CommunityService.myWall → mine, pinned). "Mine" =
     my own posts (any visibility). "Pinned" = posts I keep on my wall because I'm cheering
     for them. Reuses the same card. --%>
<div class="max-w-xl mx-auto pb-8">

  <header class="relative overflow-hidden rounded-3xl bg-gradient-to-br from-gold-500 to-gold-700 text-white p-5 shadow-sm">
    <span class="pointer-events-none absolute -right-3 -top-3 text-7xl opacity-20 select-none" aria-hidden="true">🧡</span>
    <h1 class="text-2xl font-bold leading-tight">My wall</h1>
    <p class="mt-1 text-sm text-cream-100">Your posts, and the ones you're cheering for.</p>
  </header>

  <button type="button" class="btn-cta w-full mt-4 text-lg flex items-center justify-center gap-2"
          aria-label="Share a post or give kudos"
          hx-get="/universal/showView" hx-vals='{"template": "community/form", "data[user]": "currentUser"}'
          hx-target="#content" hx-swap="innerHTML">
    <span aria-hidden="true">＋</span> Share a post or kudos
  </button>

  <%-- Pinned — the posts I'm cheering for --%>
  <g:if test="${walls.pinned}">
    <h2 class="mt-6 mb-2 text-sm font-bold uppercase tracking-wide text-stone-400 flex items-center gap-1.5"><span aria-hidden="true">📌</span> Cheering for</h2>
    <div class="space-y-3">
      <g:each in="${walls.pinned}" var="c"><g:render template="/universal/community/card" model="[card: c]"/></g:each>
    </div>
  </g:if>

  <%-- Mine --%>
  <h2 class="mt-6 mb-2 text-sm font-bold uppercase tracking-wide text-stone-400 flex items-center gap-1.5"><span aria-hidden="true">💬</span> My posts</h2>
  <g:if test="${walls.mine}">
    <div class="space-y-3">
      <g:each in="${walls.mine}" var="c"><g:render template="/universal/community/card" model="[card: c]"/></g:each>
    </div>
  </g:if>
  <g:else>
    <div class="card p-6 text-center">
      <div class="text-4xl mb-2" aria-hidden="true">🌺</div>
      <p class="text-sm text-stone-500">You haven't shared anything yet. Pin a post from the Community Wall, or share your own above.</p>
    </div>
  </g:else>

</div>
