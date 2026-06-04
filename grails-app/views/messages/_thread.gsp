<div class="flex items-center gap-3 mb-4">
    <g:if test="${staff}">
        <button hx-get="/messages/inbox"
                hx-target="#content"
                hx-swap="innerHTML"
                class="text-stone-500 hover:text-stone-700 min-h-[44px] py-2"
                aria-label="Back to inbox">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
        </button>
    </g:if>
    <div>
        <h2 class="text-xl font-bold text-stone-800">
            <g:if test="${staff}">${conversation.student?.name ?: conversation.student?.username}</g:if>
            <g:else>Messages</g:else>
        </h2>
        <p class="text-xs text-stone-400">
            <g:if test="${staff}">${conversation.student?.email ?: ''}</g:if>
            <g:else>Questions about your lessons? Send us a message.</g:else>
        </p>
    </div>
</div>

<div class="bg-white rounded-xl shadow-sm border border-stone-200 flex flex-col"
     style="height: calc(100vh - 220px); min-height: 380px;">

    <!-- Live region: connects to SSE; refetches bubbles whenever any message is created -->
    <div hx-ext="sse" sse-connect="/universal/sse" class="flex-1 min-h-0 flex flex-col">
        <div id="msg-bubbles"
             hx-get="/messages/bubbles?conversationId=${conversation.id}"
             hx-trigger="sse:Message-create"
             hx-swap="innerHTML"
             class="flex-1 min-h-0 overflow-y-auto p-4 space-y-3">
            <g:render template="/messages/bubbles" model="[messages: messages, me: me]"/>
        </div>
    </div>

    <form hx-post="/messages/send"
          hx-target="#msg-bubbles"
          hx-swap="innerHTML"
          hx-on::after-request="if(event.detail.successful){var t=this.querySelector('textarea');t.value='';t.focus();}"
          class="border-t border-stone-100 p-3 flex items-end gap-2">
        <input type="hidden" name="conversationId" value="${conversation.id}"/>
        <textarea name="body" rows="1" required placeholder="Type a message…"
                  class="flex-1 px-4 py-3 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-transparent text-base resize-none"></textarea>
        <g:render template="/universal/components/button" model="[text: 'Send', type: 'submit']"/>
    </form>
</div>
