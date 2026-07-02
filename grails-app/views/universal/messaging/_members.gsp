<%-- Channel member strip (moderators/admins). Model: m (MessageService.channelMembers).
     Horizontal-scroll avatars + count; tap a member → a shared popover (built outside the
     scroll container so it isn't clipped) with mute / kick-ban actions. Each action is
     generic CRUD on ChannelMembership that re-renders this strip. Null for non-mods. --%>
<g:if test="${m}">
<% def channelId = m.channel.id %>
<div class="flex items-center gap-2 mb-1.5">
    <span class="text-[11px] font-semibold text-stone-500 uppercase tracking-wide">Members</span>
    <span class="text-[11px] font-bold text-white bg-rose-600 rounded-full px-2 py-0.5">${m.count}</span>
    <span class="text-[11px] text-stone-400">tap to moderate</span>
</div>
<div class="member-strip">
    <g:each in="${m.members}" var="mem">
        <% String ringCls = mem.status == 'banned' ? 'ring-2 ring-rose-400' : (mem.status == 'muted' ? 'ring-2 ring-gold-400' : '') %>
        <div class="member-cell">
            <%-- Tap → load this member's actions into the bottom sheet (declarative), then open it. --%>
            <button type="button" class="rounded-full ${ringCls}"
                    hx-get="/universal/showView"
                    hx-vals='{"template":"messaging/memberActions","data[ma]":"service:messageService:memberActions","channelId":"${channelId}","userId":"${mem.userId}"}'
                    hx-target="#action-sheet-body" hx-swap="innerHTML" hx-on::after-request="openActionSheet()">
                <g:render template="/universal/components/avatar" model="[name: mem.name, size: 'h-11 w-11', textSize: 'text-sm']"/>
            </button>
            <span class="block text-[10px] text-center mt-0.5 w-12 truncate ${mem.status == 'active' ? 'text-stone-400' : (mem.status == 'muted' ? 'text-gold-700' : 'text-rose-600')}">
                ${mem.status == 'active' ? mem.name.split(' ')[0] : mem.status}
            </span>
        </div>
    </g:each>
</div>
</g:if>
