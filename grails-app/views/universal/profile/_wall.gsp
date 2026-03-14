<g:if test="${wallPosts}">
    <div class="space-y-3">
        <g:each in="${wallPosts}" var="post">
            <div class="p-3 rounded-lg bg-stone-50">
                <div class="flex items-center gap-2 mb-1">
                    <span class="text-sm font-medium text-stone-700">${post.user?.name ?: post.user?.username}</span>
                    <span class="text-xs text-stone-400"><g:formatDate date="${post.dateCreated}" format="MMM d, yyyy"/></span>
                </div>
                <p class="text-sm text-stone-600">${post.message}</p>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <p class="text-sm text-stone-400">No wall posts yet.</p>
</g:else>
