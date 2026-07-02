<%-- Chat view for a channel OR a direct message. Model: view (MessageService.channelView
     / startDm). Bubbles re-fetch on 'sse:Message-create' (everyone) + 'refreshmsgs' (the
     sender). Under the composer: moderators get the moderation strip; everyone else gets
     the "people you can message" strip (channels only — DMs have no strip). --%>
<%
    def channelsVals = '{"template": "messaging/home", "data[list]": "service:messageService:channelList", "data[user]": "currentUser"}'
    def dmsVals      = '{"template": "messaging/dms", "data[dms]": "service:messageService:dmList", "data[user]": "currentUser"}'
%>
<g:if test="${view}">
<%
    boolean isDm = view.channel.isDm
    def backVals  = isDm ? dmsVals : channelsVals
    def backLabel = isDm ? 'Messages' : 'Channels'
    def msgVals = '{"template": "messaging/messages", "data[view]": "service:messageService:channelView", "data[user]": "currentUser", "channelId": "' + view.channel.id + '"}'
%>
<div hx-ext="sse" sse-connect="/universal/sse">
    <button class="text-stone-400 hover:text-rose-700 text-sm mb-3" hx-get="/universal/showView" hx-vals='${backVals}'
            hx-target="#content" hx-swap="innerHTML">&larr; ${backLabel}</button>

    <div class="flex items-center gap-2 mb-3">
        <g:if test="${isDm}">
            <g:render template="/universal/components/avatar" model="[name: view.channel.name, size: 'h-9 w-9', textSize: 'text-sm']"/>
        </g:if>
        <g:else>
            <span class="h-8 w-8 rounded-full bg-rose-100 text-rose-700 font-bold flex items-center justify-center">#</span>
        </g:else>
        <div class="min-w-0">
            <h2 class="font-bold text-stone-800 truncate">${view.channel.name}
                <g:if test="${view.channel.visibility == 'staff'}"><span class="ml-1 align-middle"><g:render template="/universal/components/pill" model="[label: 'Staff', size: 'xs']"/></span></g:if>
                <g:if test="${isDm}"><span class="ml-1 align-middle"><g:render template="/universal/components/pill" model="[label: 'Direct', tone: 'gold', size: 'xs']"/></span></g:if>
            </h2>
            <g:if test="${!isDm && view.channel.description}"><p class="text-xs text-stone-400 truncate">${view.channel.description}</p></g:if>
        </div>
    </div>

    <div id="msg-bubbles" class="card p-3 h-[55vh] md:h-[60vh] overflow-y-auto bg-cream-50"
         hx-get="/universal/showView" hx-vals='${msgVals}'
         hx-trigger="sse:Message-create, refreshmsgs" hx-swap="innerHTML">
        <g:render template="/universal/messaging/messages" model="[view: view]"/>
    </div>

    <g:if test="${view.canPost}">
        <form class="mt-3 flex items-end gap-2" data-chat-composer
              hx-post="/universal/save?domainName=Message" hx-vals='{"template": ""}' hx-swap="none">
            <input type="hidden" name="channel.id" value="${view.channel.id}"/>
            <input name="body" required autocomplete="off" placeholder="Message ${isDm ? view.channel.name : '#' + view.channel.name}…"
                   class="flex-1 px-4 py-3 rounded-2xl bg-white border border-cream-300 text-stone-800 placeholder-stone-400 focus:border-rose-500 focus:ring-1 focus:ring-rose-500 focus:outline-none min-h-[44px]"/>
            <button type="submit" class="h-11 w-11 shrink-0 rounded-full bg-rose-600 hover:bg-rose-700 text-white flex items-center justify-center" aria-label="Send">
                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 12h14M13 6l6 6-6 6"/></svg>
            </button>
        </form>
    </g:if>
    <g:elseif test="${view.muted}">
        <p class="mt-3 text-center text-xs text-rose-600">You've been muted in this channel — you can read but not post.</p>
    </g:elseif>
    <g:elseif test="${isDm}">
        <p class="mt-3 text-center text-xs text-stone-400">You can't message this person right now.</p>
    </g:elseif>
    <g:else>
        <p class="mt-3 text-center text-xs text-stone-400">You can read this channel but not post to it.</p>
    </g:else>

    <%-- Member strip: moderators see the moderation strip; everyone else (non-DM) sees
         the "people you can message" strip. Both refetch on their own event. --%>
    <g:if test="${view.canModerate}">
        <div id="member-strip" class="mt-3 pt-3 border-t border-cream-200"
             hx-get="/universal/showView"
             hx-vals='{"template":"messaging/members","data[m]":"service:messageService:channelMembers","channelId":"${view.channel.id}"}'
             hx-trigger="load, memberschanged from:body" hx-swap="innerHTML"></div>
    </g:if>
    <g:elseif test="${!isDm}">
        <div id="people-strip" class="mt-3 pt-3 border-t border-cream-200"
             hx-get="/universal/showView"
             hx-vals='{"template":"messaging/people","data[pe]":"service:messageService:channelPeople","channelId":"${view.channel.id}"}'
             hx-trigger="load, peoplechanged from:body" hx-swap="innerHTML"></div>
    </g:elseif>

    <%-- Shared action bottom-sheet (moderation actions OR person PM/block actions). --%>
    <g:if test="${view.canModerate || !isDm}">
        <div id="action-sheet-overlay" class="hidden fixed inset-0 z-40 bg-black/40" onclick="closeActionSheet()"></div>
        <div id="action-sheet" class="hidden fixed bottom-0 inset-x-0 z-50 bg-white rounded-t-3xl p-4 pb-8 translate-y-full transition-transform max-w-lg mx-auto">
            <div class="mx-auto mb-3 h-1.5 w-10 rounded-full bg-cream-300"></div>
            <div id="action-sheet-body"></div>
        </div>
    </g:if>
</div>
</g:if>
<g:else>
    <div>
        <button class="text-stone-400 hover:text-rose-700 text-sm mb-4" hx-get="/universal/showView" hx-vals='${channelsVals}'
                hx-target="#content" hx-swap="innerHTML">&larr; Channels</button>
        <g:render template="/universal/components/emptyState" model="[message: 'Conversation unavailable']"/>
    </div>
</g:else>
