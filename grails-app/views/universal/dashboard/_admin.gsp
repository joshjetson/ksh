<div class="flex items-center justify-between mb-6 gap-3">
    <h2 class="text-xl font-bold text-stone-800">Dashboard</h2>
    <select name="period"
            hx-get="/universal/showView"
            hx-vals='{"template": "dashboard/metrics", "data[stats]": "service:dashboardService:adminStats"}'
            hx-target="#dash-metrics"
            hx-swap="innerHTML"
            hx-trigger="change"
            class="px-3 py-2 border border-stone-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-rose-500 bg-white">
        <option value="week">Last 7 days</option>
        <option value="month" selected>Last 30 days</option>
        <option value="year">Last 12 months</option>
        <option value="all">All time</option>
    </select>
</div>

<div id="dash-metrics"
     hx-get="/universal/showView"
     hx-vals='{"template": "dashboard/metrics", "data[stats]": "service:dashboardService:adminStats", "period": "month"}'
     hx-trigger="load"
     hx-swap="innerHTML">
    <div class="text-center py-12 text-sm text-stone-400">Loading metrics…</div>
</div>
