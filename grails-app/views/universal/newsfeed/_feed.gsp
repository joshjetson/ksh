<h2 class="text-xl font-bold text-stone-800 mb-6">Newsfeed</h2>

<g:if test="${posts}">
    <div class="space-y-4">
        <g:each in="${posts}" var="post">
            <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-4">
                <div class="flex items-center gap-3 mb-2">
                    <div class="w-10 h-10 rounded-full bg-rose-100 flex items-center justify-center flex-shrink-0">
                        <span class="text-sm font-bold text-rose-700">${(post.user?.name ?: post.user?.username)?.charAt(0)?.toUpperCase()}</span>
                    </div>
                    <div>
                        <span class="text-sm font-semibold text-stone-800">${post.user?.name ?: post.user?.username}</span>
                        <p class="text-xs text-stone-400"><g:formatDate date="${post.dateCreated}" format="MMM d, yyyy"/></p>
                    </div>
                </div>
                <p class="text-sm text-stone-600">${post.message}</p>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <g:render template="/universal/components/emptyState" model="[message: 'No posts yet. Check back soon!']"/>
</g:else>
