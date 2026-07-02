<%-- Community search results. Model: res (CommunityService.searchPosts → q, cards). --%>
<g:if test="${res?.cards}">
  <div class="space-y-3">
    <g:each in="${res.cards}" var="c"><g:render template="/universal/community/card" model="[card: c]"/></g:each>
  </div>
</g:if>
<g:elseif test="${res?.q}">
  <p class="text-sm text-stone-400 py-6 text-center">No posts match &ldquo;${res.q}&rdquo;.</p>
</g:elseif>
<g:else>
  <g:render template="/universal/components/emptyState" model="[message: 'No posts yet']"/>
</g:else>
