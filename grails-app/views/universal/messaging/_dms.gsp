<%-- Direct messages list (the "pm's" view). Model: dms (MessageService.dmList), user.
     Tap a thread → open the DM (channelView). Start new DMs by tapping a person in a
     channel's member strip. --%>
<% def channelsVals = '{"template":"messaging/home","data[list]":"service:messageService:channelList","data[user]":"currentUser"}' %>
<div>
    <button class="text-stone-400 hover:text-rose-700 text-sm mb-3" hx-get="/universal/showView" hx-vals='${channelsVals}'
            hx-target="#content" hx-swap="innerHTML">&larr; Channels</button>
    <h2 class="text-xl font-bold text-stone-800 mb-1">Direct messages</h2>
    <p class="text-sm text-stone-400 mb-5">Private conversations. Start one by tapping a person under a channel.</p>

    <g:if test="${dms?.threads}">
        <div class="space-y-2">
            <g:each in="${dms.threads}" var="t">
                <button type="button" class="card p-3 w-full text-left hover:bg-cream-50 flex items-center gap-3"
                        hx-get="/universal/showView"
                        hx-vals='{"template":"messaging/channel","data[view]":"service:messageService:channelView","data[user]":"currentUser","channelId":"${t.id}"}'
                        hx-target="#content" hx-swap="innerHTML">
                    <g:render template="/universal/components/avatar" model="[name: t.name, size: 'h-11 w-11', textSize: 'text-base']"/>
                    <span class="min-w-0 flex-1">
                        <span class="block font-semibold text-stone-800 truncate">${t.name}</span>
                        <g:if test="${t.last}"><span class="block text-sm text-stone-400 truncate">${t.last}</span></g:if>
                    </span>
                    <g:if test="${t.time}"><span class="text-[11px] text-stone-400 shrink-0"><g:formatDate date="${t.time}" format="MMM d"/></span></g:if>
                </button>
            </g:each>
        </div>
    </g:if>
    <g:else>
        <g:render template="/universal/components/emptyState" model="[message: 'No direct messages yet', hint: 'Open a channel and tap someone in the member strip to start a private chat.']"/>
    </g:else>
</div>
