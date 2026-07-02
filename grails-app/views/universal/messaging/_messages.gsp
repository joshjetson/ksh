<%-- Message bubbles for a channel. Model: view (MessageService.channelView map) — the
     same shape whether rendered inline by _channel or refetched via showView. Own
     messages right-aligned (rose); others left with author + time. Re-rendered on every
     SSE/refresh ping. Ends by sticking the scroll to the bottom. --%>
<% def messages = view?.messages; def meId = view?.meId %>
<g:if test="${messages}">
    <div class="space-y-2.5">
        <g:each in="${messages}" var="m">
            <% boolean mine = (meId != null && m.authorId == meId) %>
            <div class="flex gap-2 ${mine ? 'justify-end' : 'justify-start'}">
                <%-- Sender stamp (avatar) on others' messages --%>
                <g:if test="${!mine}">
                    <g:render template="/universal/components/avatar" model="[name: m.authorName, size: 'h-8 w-8', textSize: 'text-xs']"/>
                </g:if>
                <div class="max-w-[78%] ${mine ? 'items-end' : 'items-start'} flex flex-col">
                    <g:if test="${!mine}"><span class="text-[11px] font-medium text-stone-500 px-1 mb-0.5">${m.authorName}</span></g:if>
                    <div class="px-3 py-2 rounded-2xl text-sm whitespace-pre-line break-words ${mine ? 'bg-rose-600 text-white rounded-br-md' : 'bg-white border border-cream-200 text-stone-800 rounded-bl-md'}">${m.body}</div>
                    <span class="text-[10px] text-stone-400 px-1 mt-0.5"><g:formatDate date="${m.time}" format="MMM d · h:mm a"/></span>
                </div>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <div class="h-full flex items-center justify-center">
        <p class="text-sm text-stone-400">No messages yet — say hello! 👋</p>
    </div>
</g:else>
<script>
    (function () { var s = document.getElementById('msg-bubbles'); if (s) s.scrollTop = s.scrollHeight; })();
</script>
