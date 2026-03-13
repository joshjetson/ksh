<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Korean School House</title>
    <style>
        .tab-active { color: #be123c; border-bottom-color: #be123c; }
        .tab-btn { border-bottom: 2px solid transparent; }
        .tab-btn:hover { color: #78716c; }
    </style>
</head>
<body>

<!-- Single nav bar -->
<header class="bg-white shadow-sm sticky top-0 z-20">
    <div class="max-w-6xl mx-auto px-4">
        <!-- Top row: title + logout -->
        <div class="flex justify-between items-center h-12">
            <span class="text-base font-bold text-stone-800">&#54620; Korean School House</span>
            <a href="/logout" class="text-sm text-stone-500 hover:text-stone-700 py-2 px-3">Logout</a>
        </div>
        <!-- Tab row -->
        <nav class="flex gap-1 overflow-x-auto -mb-px" id="main-nav">
            <button class="tab-btn tab-active px-4 py-3 text-sm font-medium whitespace-nowrap min-h-[44px]"
                    hx-get="/universal/showView"
                    hx-vals='{"template": "courses/browse", "data[courses]": "list:Course", "data[user]": "currentUser"}'
                    hx-target="#content"
                    hx-swap="innerHTML"
                    onclick="setActiveTab(this)">
                Browse
            </button>
            <button class="tab-btn px-4 py-3 text-sm font-medium whitespace-nowrap text-stone-500 min-h-[44px]"
                    hx-get="/universal/showView"
                    hx-vals='{"template": "courses/myCourses", "data[user]": "currentUser"}'
                    hx-target="#content"
                    hx-swap="innerHTML"
                    onclick="setActiveTab(this)">
                My Courses
            </button>
            <sec:ifAnyGranted roles='ROLE_ADMIN,ROLE_TEACHER'>
                <button class="tab-btn px-4 py-3 text-sm font-medium whitespace-nowrap text-stone-500 min-h-[44px]"
                        hx-get="/universal/showView"
                        hx-vals='{"template": "lessons/manage", "data[user]": "currentUser"}'
                        hx-target="#content"
                        hx-swap="innerHTML"
                        onclick="setActiveTab(this)">
                    Lessons
                </button>
            </sec:ifAnyGranted>
            <button class="tab-btn px-4 py-3 text-sm font-medium whitespace-nowrap text-stone-500 min-h-[44px]"
                    hx-get="/universal/showView"
                    hx-vals='{"template": "profile/view", "data[user]": "currentUser"}'
                    hx-target="#content"
                    hx-swap="innerHTML"
                    onclick="setActiveTab(this)">
                Profile
            </button>
        </nav>
    </div>
</header>

<!-- Content area -->
<div class="max-w-6xl mx-auto px-4">
    <div id="content" class="py-4"
         hx-get="/universal/showView"
         hx-vals='{"template": "courses/browse", "data[courses]": "list:Course", "data[user]": "currentUser"}'
         hx-trigger="load"
         hx-swap="innerHTML">
    </div>
</div>

<script>
    function setActiveTab(el) {
        document.querySelectorAll('#main-nav .tab-btn').forEach(function(btn) {
            btn.classList.remove('tab-active');
            btn.classList.add('text-stone-500');
        });
        el.classList.add('tab-active');
        el.classList.remove('text-stone-500');
    }
</script>

</body>
</html>
