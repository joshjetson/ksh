<%-- Message search results. Model: res (MessageService.searchMessages → q, results[]).
     Access-checked server-side (only the caller's visible channels/DMs, blocked authors
     filtered). Each hit deep-links into its channel. A blank query renders nothing so the
     channel list below stays visible. --%>
<g:if test="${res?.results}">
    <div class="mb-5">
        <p class="text-xs font-semibold text-stone-400 uppercase tracking-wide mb-2">Messages matching &ldquo;${res.q}&rdquo;</p>
        <div class="space-y-2">
            <g:each in="${res.results}" var="m">
                <button type="button" class="card p-3 w-full text-left hover:bg-cream-50"
                        hx-get="/universal/showView"
                        hx-vals='{"template": "messaging/channel", "data[view]": "service:messageService:channelView", "data[user]": "currentUser", "channelId": "${m.channelId}"}'
                        hx-target="#content" hx-swap="innerHTML">
                    <span class="flex items-center gap-2 mb-0.5">
                        <span class="text-xs font-semibold text-rose-700 truncate">${m.isDm ? m.channelName : '#' + m.channelName}</span>
                        <span class="text-[11px] text-stone-400 shrink-0">${m.authorName} &middot; <g:formatDate date="${m.time}" format="MMM d"/></span>
                    </span>
                    <span class="block text-sm text-stone-700 line-clamp-2">${m.body}</span>
                </button>
            </g:each>
        </div>
    </div>
</g:if>
<g:elseif test="${res?.q}">
    <p class="text-sm text-stone-400 mb-5">No messages match &ldquo;${res.q}&rdquo;.</p>
</g:elseif>
