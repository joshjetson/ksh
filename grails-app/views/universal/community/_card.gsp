<%-- Community wall card. Model: card (Map from CommunityService). One post or kudos with a
     bouncy cheer button, a pin (or visibility badge when mine), and a compact moderator row.
     Every action re-renders just this card via service:communityService:postCard (quiet=true
     → no toast). `post.id` is always sent — the save uses it, the delete ignores it. Renders
     nothing when card is null (so a deleted card vanishes on swap). --%>
<g:if test="${card}">
<%
    boolean kudos = card.kind == 'kudos'
    String surface  = kudos ? 'bg-gold-500/5 border-gold-500/25' : 'bg-rose-50 border-rose-100'
    String mark     = kudos ? '🎉' : '💬'
    String markRing = kudos ? 'bg-gold-500/15' : 'bg-rose-600/10'
    String markLbl  = kudos ? 'Kudos' : 'Community post'
    def visMap = [public: [icon: '🌏', label: 'Public', tone: 'rose'],
                  staff:  [icon: '🛡️', label: 'Staff only', tone: 'cream'],
                  self:   [icon: '🔒', label: 'Just me', tone: 'cream']]
    def vis = visMap[card.visibility] ?: visMap.public
    String cheerLbl = card.cheerCount == 1 ? '1 cheering' : "${card.cheerCount} cheering"
    // Re-render payload shared by every action on this card.
    // NB: the re-render id param is `cardId`, NOT `postId` — `postId` collides with GORM's
    // read-only FK accessor on PostCheer/PostPin and the binder rejects it on save.
    String rerender = '{"template": "community/card", "data[card]": "service:communityService:postCard", "cardId": "' + card.id + '", "post.id": "' + card.id + '", "quiet": "true"}'
%>
<div id="post-card-${card.id}" class="${card.hidden ? 'opacity-60' : ''}">
  <article class="card ${surface} p-4">

    <g:if test="${card.hidden}">
      <div class="mb-3 inline-flex items-center gap-1.5 text-xs font-semibold text-stone-400">
        <span aria-hidden="true">🙈</span> Hidden from the wall
      </div>
    </g:if>

    <div class="flex items-center gap-3">
      <g:if test="${card.isAnonymous}">
        <span class="h-9 w-9 rounded-full bg-rose-100 flex items-center justify-center text-lg shrink-0" aria-label="Shared anonymously">🌺</span>
      </g:if>
      <g:else>
        <g:render template="/universal/components/avatar" model="[name: card.authorName]"/>
      </g:else>
      <div class="min-w-0 flex-1">
        <p class="font-semibold text-stone-800 truncate">${card.authorName}</p>
        <p class="text-xs text-stone-400"><g:formatDate date="${card.createdAt}" format="MMM d"/></p>
      </div>
      <span class="h-9 w-9 rounded-full ${markRing} flex items-center justify-center text-lg shrink-0" aria-label="${markLbl}" title="${markLbl}">${mark}</span>
    </div>

    <h3 class="mt-3 font-bold text-stone-800 leading-snug">${card.title}</h3>
    <p class="mt-1 text-[15px] text-stone-600 leading-relaxed whitespace-pre-line">${card.body}</p>

    <div class="mt-4 flex items-center gap-2">

      <%-- 👏 cheer — tap to send/withdraw; re-renders this card --%>
      <button type="button" class="cheer-btn ${card.iCheered ? 'cheer-btn--active' : ''}"
              aria-pressed="${card.iCheered}"
              aria-label="${card.iCheered ? 'You are cheering — tap to undo. ' : 'Send a cheer. '}${cheerLbl}"
              hx-post="${card.iCheered ? '/universal/delete/' + card.myCheerId + '?domainName=PostCheer' : '/universal/save?domainName=PostCheer'}"
              hx-vals='${rerender}' hx-target="#post-card-${card.id}" hx-swap="outerHTML">
        <span class="cheer-emoji text-base leading-none" aria-hidden="true">👏</span>
        <span class="tabular-nums">${card.cheerCount}</span>
        <span>${card.iCheered ? 'Cheering' : 'Cheer'}</span>
      </button>

      <span class="flex-1"></span>

      <g:if test="${card.isMine}">
        <span class="inline-flex items-center gap-1.5 px-3 py-2 rounded-full text-sm font-semibold text-stone-500 bg-white border border-cream-300" title="Who can see this">
          <span aria-hidden="true">${vis.icon}</span><span>${vis.label}</span>
        </span>
      </g:if>
      <g:else>
        <%-- 📌 pin to my wall --%>
        <button type="button" class="pin-btn ${card.iPinned ? 'pin-btn--active' : ''}"
                aria-pressed="${card.iPinned}"
                aria-label="${card.iPinned ? 'Pinned to your wall — tap to unpin' : 'Pin to your wall'}"
                hx-post="${card.iPinned ? '/universal/delete/' + card.myPinId + '?domainName=PostPin' : '/universal/save?domainName=PostPin'}"
                hx-vals='${rerender}' hx-target="#post-card-${card.id}" hx-swap="outerHTML">
          <span class="pin-emoji text-base leading-none" aria-hidden="true">📌</span>
          <span>${card.iPinned ? 'Pinned' : 'Pin'}</span>
        </button>
      </g:else>
    </div>

    <g:if test="${card.canModerate}">
      <div class="mt-3 pt-3 border-t border-cream-200 flex items-center gap-1">
        <span class="text-[11px] font-semibold uppercase tracking-wide text-stone-300 mr-auto">Moderator</span>
        <button type="button" class="px-3 py-2 min-h-[44px] rounded-lg text-xs font-semibold text-stone-500 hover:bg-cream-100 transition-colors"
                aria-label="${card.hidden ? 'Unhide this post' : 'Hide this post from the wall'}"
                hx-post="/universal/update/${card.id}?domainName=CommunityPost"
                hx-vals='{"template": "community/card", "data[card]": "service:communityService:postCard", "cardId": "${card.id}", "hidden": "${card.hidden ? 'false' : 'true'}", "quiet": "true"}'
                hx-target="#post-card-${card.id}" hx-swap="outerHTML">${card.hidden ? '👁 Unhide' : '🙈 Hide'}</button>
        <button type="button" class="px-3 py-2 min-h-[44px] rounded-lg text-xs font-semibold text-stone-500 hover:bg-cream-100 transition-colors"
                aria-label="Edit this post"
                hx-get="/universal/showView"
                hx-vals='{"template": "community/form", "data[item]": "get:CommunityPost:pId", "pId": "${card.id}", "data[user]": "currentUser"}'
                hx-target="#content" hx-swap="innerHTML">✏️ Edit</button>
        <button type="button" class="px-3 py-2 min-h-[44px] rounded-lg text-xs font-semibold text-rose-600 hover:bg-rose-50 transition-colors"
                aria-label="Delete this post"
                hx-post="/universal/delete/${card.id}?domainName=CommunityPost"
                hx-vals='${rerender}' hx-target="#post-card-${card.id}" hx-swap="outerHTML"
                hx-confirm="Remove this from the wall permanently?">🗑 Delete</button>
      </div>
    </g:if>

  </article>
</div>
</g:if>
