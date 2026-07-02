<%-- Moderator post search (managers only). Model: res (CommunityService.searchPosts → q,
     cards), user. Searches ALL posts ever — any visibility, including hidden. --%>
<div class="max-w-xl mx-auto pb-8">
  <div class="mb-4">
    <h1 class="text-xl font-bold text-stone-800">Search posts &amp; kudos</h1>
    <p class="text-sm text-stone-400">Every post ever shared — including hidden and private ones.</p>
  </div>
  <g:render template="/universal/components/searchBar"
            model="[target: 'community-search-results', placeholder: 'Search all posts…',
                    vals: '{\"template\": \"community/searchResults\", \"data[res]\": \"service:communityService:searchPosts\"}']"/>
  <div id="community-search-results">
    <g:render template="/universal/community/searchResults" model="[res: res]"/>
  </div>
</div>
