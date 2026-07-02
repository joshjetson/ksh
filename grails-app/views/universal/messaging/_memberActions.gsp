<%-- Moderation actions for one channel member (bottom-sheet body). Model: ma
     (MessageService.memberActions). Plain declarative hx-post buttons — each sets a
     ChannelMembership status; on success it fires 'memberschanged' (the strip refetches
     itself) and closes the sheet. No JS request API. --%>
<g:if test="${ma}">
<%
    def url = ma.membershipId ? "/universal/update/${ma.membershipId}" : "/universal/save"
    // create needs the channel+user; update only the status (the row exists). template:""
    // overrides the inherited #content template so the save doesn't re-render/toast — the
    // strip refetches itself via the 'memberschanged' event below.
    def base = ma.membershipId ? '' : ('"channel.id":"' + ma.channelId + '","user.id":"' + ma.userId + '",')
    def tail = '"template":""}'
    def after = "if(event.detail.successful){document.body.dispatchEvent(new Event('memberschanged'));closeActionSheet()}"
%>
<div class="flex items-center gap-3 mb-3">
    <g:render template="/universal/components/avatar" model="[name: ma.name, size: 'h-11 w-11', textSize: 'text-base']"/>
    <div class="min-w-0">
        <p class="font-semibold text-stone-800 truncate">${ma.name}</p>
        <p class="text-xs ${ma.status == 'active' ? 'text-stone-400' : (ma.status == 'muted' ? 'text-gold-700' : 'text-rose-600')}">${ma.status == 'active' ? 'Active in this channel' : ma.status.capitalize()}</p>
    </div>
</div>

<div class="space-y-1">
    <%-- PM action — moderators/admins can message anyone (incl. other staff/admins). --%>
    <g:if test="${ma.canMessage}">
        <button class="member-act text-rose-700" hx-get="/universal/showView"
                hx-vals='{"template":"messaging/channel","data[view]":"service:messageService:startDm","data[user]":"currentUser","targetUserId":"${ma.userId}"}'
                hx-target="#content" hx-swap="innerHTML" hx-on::after-request="closeActionSheet()">💬 Message</button>
    </g:if>
</div>

<g:if test="${ma.isAdmin}">
    <p class="text-sm text-stone-400 px-1 pt-2">Admins can't be moderated.</p>
</g:if>
<g:else>
    <div class="space-y-1 mt-1">
        <g:if test="${ma.status != 'muted'}">
            <button class="member-act" hx-post="${url}?domainName=ChannelMembership"
                    hx-vals='{${base}"status":"muted",${tail}' hx-swap="none" hx-on::after-request="${after}">🔇 Mute — can read, can't post</button>
        </g:if>
        <g:else>
            <button class="member-act" hx-post="${url}?domainName=ChannelMembership"
                    hx-vals='{${base}"status":"active",${tail}' hx-swap="none" hx-on::after-request="${after}">🔊 Unmute</button>
        </g:else>

        <g:if test="${ma.status != 'banned'}">
            <button class="member-act text-rose-600" hx-post="${url}?domainName=ChannelMembership"
                    hx-vals='{${base}"status":"banned",${tail}' hx-swap="none" hx-confirm="Remove ${ma.name} from this channel?"
                    hx-on::after-request="${after}">⛔ Kick / Ban — no access</button>
        </g:if>
        <g:else>
            <button class="member-act" hx-post="${url}?domainName=ChannelMembership"
                    hx-vals='{${base}"status":"active",${tail}' hx-swap="none" hx-on::after-request="${after}">✅ Unban</button>
        </g:else>
    </div>
</g:else>
</g:if>
