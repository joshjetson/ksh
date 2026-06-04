<g:if test="${messages}">
    <g:each in="${messages}" var="m">
        <% def mine = (m.sender?.id == me?.id) %>
        <div class="flex ${mine ? 'justify-end' : 'justify-start'}">
            <div class="max-w-[75%]">
                <g:if test="${!mine}">
                    <p class="text-xs text-stone-400 mb-0.5 ml-1">${m.sender?.name ?: m.sender?.username}</p>
                </g:if>
                <div class="px-4 py-2 rounded-2xl ${mine ? 'bg-rose-700 text-white rounded-br-sm' : 'bg-stone-100 text-stone-800 rounded-bl-sm'}">
                    <p class="text-sm whitespace-pre-wrap break-words">${m.body}</p>
                </div>
                <p class="text-[10px] text-stone-400 mt-0.5 ${mine ? 'text-right mr-1' : 'ml-1'}">
                    <g:formatDate date="${m.dateCreated}" format="MMM d, h:mm a"/>
                </p>
            </div>
        </div>
    </g:each>
</g:if>
<g:else>
    <p class="text-center text-sm text-stone-400 py-8">No messages yet. Say hello! 👋</p>
</g:else>

<script>
    // Keep the conversation pinned to the newest message after each (re)render.
    (function () {
        var el = document.getElementById('msg-bubbles');
        if (el) { el.scrollTop = el.scrollHeight; }
    })();
</script>
