<%-- The Community Wall. Model: wall (CommunityService.generalWall → weekLabel, postCount,
     kudosCount, cards). Public (and, for staff, staff-only) posts from this week. --%>
<div class="max-w-xl mx-auto pb-8">

  <header class="relative overflow-hidden rounded-3xl bg-gradient-to-br from-rose-600 to-rose-800 text-white p-5 shadow-sm">
    <span class="pointer-events-none absolute -right-3 -top-3 text-7xl opacity-15 select-none" aria-hidden="true">📣</span>
    <p class="text-xs font-semibold uppercase tracking-wide text-rose-200">${wall.weekLabel}</p>
    <h1 class="mt-1 text-2xl font-bold leading-tight">This week's posts &amp; kudos 👏</h1>
    <p class="mt-2 text-sm text-rose-100">
      <span class="font-bold text-white tabular-nums">${wall.postCount}</span> posts
      <span class="opacity-40">·</span>
      <span class="font-bold text-gold-400 tabular-nums">${wall.kudosCount}</span> kudos
    </p>
  </header>

  <button type="button" class="btn-cta w-full mt-4 text-lg flex items-center justify-center gap-2"
          aria-label="Share a post or give kudos"
          hx-get="/universal/showView" hx-vals='{"template": "community/form", "data[user]": "currentUser"}'
          hx-target="#content" hx-swap="innerHTML">
    <span aria-hidden="true">＋</span> Share a post or kudos
  </button>

  <g:if test="${wall.cards}">
    <div class="space-y-3 mt-5">
      <g:each in="${wall.cards}" var="c">
        <g:render template="/universal/community/card" model="[card: c]"/>
      </g:each>
    </div>
  </g:if>
  <g:else>
    <div class="text-center py-16 px-6">
      <div class="text-6xl mb-3" aria-hidden="true">🌺</div>
      <p class="font-semibold text-stone-700">The wall is peaceful and quiet</p>
      <p class="mt-1 text-sm text-stone-400 max-w-xs mx-auto">Be the first to share a post or give a classmate kudos this week. 화이팅!</p>
    </div>
  </g:else>

</div>
