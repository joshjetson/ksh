<%-- Reusable declarative search input (FTS-backed). Debounced HTMX GET → showView renders
     a results partial into #${target}. The typed text is sent as the `q` request param
     (htmx includes the input's own value), which the data instruction reads, e.g.
       data[items]=fts:Course:search_fts:q            (generic whitelisted domain)
       data[res]=service:messageService:searchMessages (access-controlled service)
     Model:
       vals        — showView hx-vals JSON (template + data[..] instruction reading `q`)
       target      — id of the results container to swap (no leading '#')
       placeholder — input placeholder (optional)
       name        — query param name (default 'q'; keep aligned with the instruction) --%>
<div class="relative mb-4">
    <svg class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-stone-400 pointer-events-none"
         fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="M21 21l-4.3-4.3M11 18a7 7 0 100-14 7 7 0 000 14z"/>
    </svg>
    <input type="search" name="${name ?: 'q'}" placeholder="${placeholder ?: 'Search…'}" autocomplete="off"
           class="w-full pl-10 pr-4 py-3 rounded-xl bg-white border border-cream-300 text-base text-stone-800 placeholder-stone-400 focus:border-rose-500 focus:ring-1 focus:ring-rose-500 focus:outline-none"
           hx-get="/universal/showView" hx-vals='${vals}'
           hx-trigger="input changed delay:300ms, search"
           hx-target="#${target}" hx-swap="innerHTML"/>
</div>
