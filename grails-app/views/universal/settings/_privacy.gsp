<%-- Self-service privacy. Model: settings (my UserSetting row via owner filter),
     blocks (my UserBlock rows), user. The discoverable toggle upserts my UserSetting;
     unblock deletes a UserBlock — all owner-owned generic CRUD. The re-render payload
     targets the whole settings shell so the tab strip stays. --%>
<%
    def setting = settings ? settings[0] : null
    def url     = setting?.id ? "/universal/update/${setting.id}" : "/universal/save"
    boolean on  = setting?.discoverable
    def re = '{"template":"settings/privacy","data[user]":"currentUser","data[settings]":"filter:UserSetting:user.id=currentUserId","data[blocks]":"filter:UserBlock:owner.id=currentUserId"}'
%>
<div class="max-w-lg">
    <div class="card p-5 mb-4">
        <h3 class="font-semibold text-stone-700 mb-1">Direct messages</h3>
        <form hx-post="${url}?domainName=UserSetting" hx-trigger="change" hx-vals='${re}'
              hx-target="#settings-content" hx-swap="innerHTML">
            <label class="flex items-start justify-between gap-3 cursor-pointer py-1">
                <span>
                    <span class="block text-sm font-medium text-stone-700">Discoverable</span>
                    <span class="block text-xs text-stone-400">Let other students find you and start a private message. Teachers and admins can always reach you.</span>
                </span>
                <span class="shrink-0 pt-0.5">
                    <input type="hidden" name="_discoverable"/>
                    <input type="checkbox" name="discoverable" value="true" ${on ? 'checked' : ''}
                           class="h-5 w-5 rounded border-cream-300 text-rose-700 focus:ring-rose-500"/>
                </span>
            </label>
        </form>
    </div>

    <div class="card p-5">
        <h3 class="font-semibold text-stone-700 mb-3">Blocked people</h3>
        <g:if test="${blocks}">
            <div class="space-y-2">
                <g:each in="${blocks}" var="blk">
                    <div class="flex items-center gap-3">
                        <g:render template="/universal/components/avatar" model="[name: (blk.blocked.name ?: blk.blocked.username), size: 'h-9 w-9', textSize: 'text-sm']"/>
                        <span class="flex-1 min-w-0 text-sm font-medium text-stone-700 truncate">${blk.blocked.name ?: blk.blocked.username}</span>
                        <button class="py-1.5 px-3 text-sm rounded-full font-semibold text-rose-700 hover:bg-cream-100 min-h-[40px]"
                                hx-post="/universal/delete/${blk.id}?domainName=UserBlock" hx-vals='${re}'
                                hx-target="#settings-content" hx-swap="innerHTML">Unblock</button>
                    </div>
                </g:each>
            </div>
        </g:if>
        <g:else>
            <p class="text-sm text-stone-400">You haven't blocked anyone. You can block someone from a channel's member strip.</p>
        </g:else>
    </div>
</div>
