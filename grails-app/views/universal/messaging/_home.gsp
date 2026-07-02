<%-- Channel list. Model: list (MessageService.channelList → channels, canManage), user.
     Tap a channel → chat view (drill-in). Admin can add channels. The list live-refreshes
     when a channel is created (SSE). --%>
<%
    def listVals = '{"template": "messaging/home", "data[list]": "service:messageService:channelList", "data[user]": "currentUser"}'
%>
<div>
    <div class="flex items-center justify-between mb-5">
        <div>
            <h2 class="text-xl font-bold text-stone-800">Messages</h2>
            <p class="text-sm text-stone-400">School channels — chat in real time.</p>
        </div>
        <div class="flex items-center gap-2">
            <button class="btn-secondary btn-sm" hx-get="/universal/showView"
                    hx-vals='{"template":"messaging/dms","data[dms]":"service:messageService:dmList","data[user]":"currentUser"}'
                    hx-target="#content" hx-swap="innerHTML">Direct messages</button>
            <g:if test="${list?.canManage}">
                <button class="btn-cta btn-sm" hx-get="/universal/showView" hx-vals='{"template": "messaging/channelForm"}'
                        hx-target="#content" hx-swap="innerHTML">+ New channel</button>
            </g:if>
        </div>
    </div>

    <g:render template="/universal/components/searchBar"
              model="[target: 'msg-search-results', placeholder: 'Search messages…',
                      vals: '{\"template\": \"messaging/searchResults\", \"data[res]\": \"service:messageService:searchMessages\"}']"/>
    <div id="msg-search-results"></div>

    <div hx-ext="sse" sse-connect="/universal/sse">
        <div id="chan-list" hx-get="/universal/showView" hx-vals='${listVals}'
             hx-trigger="sse:Channel-create, sse:Channel-delete" hx-target="#chan-list" hx-swap="innerHTML">
            <g:if test="${list?.channels}">
                <div class="space-y-2">
                    <g:each in="${list.channels}" var="ch">
                        <button type="button" class="card p-4 w-full text-left hover:bg-cream-50 flex items-center gap-3"
                                hx-get="/universal/showView"
                                hx-vals='{"template": "messaging/channel", "data[view]": "service:messageService:channelView", "data[user]": "currentUser", "channelId": "${ch.id}"}'
                                hx-target="#content" hx-swap="innerHTML">
                            <span class="h-9 w-9 rounded-full bg-rose-100 text-rose-700 font-bold flex items-center justify-center shrink-0">#</span>
                            <span class="min-w-0 flex-1">
                                <span class="flex items-center gap-2">
                                    <span class="font-semibold text-stone-800 truncate">${ch.name}</span>
                                    <g:if test="${ch.visibility == 'staff'}"><g:render template="/universal/components/pill" model="[label: 'Staff', size: 'xs']"/></g:if>
                                </span>
                                <g:if test="${ch.description}"><span class="block text-sm text-stone-400 truncate">${ch.description}</span></g:if>
                            </span>
                            <span class="text-stone-300">›</span>
                        </button>
                    </g:each>
                </div>
            </g:if>
            <g:else>
                <g:render template="/universal/components/emptyState" model="[message: 'No channels yet']"/>
            </g:else>
        </div>
    </div>
</div>
