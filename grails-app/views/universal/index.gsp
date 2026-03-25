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

<% def label = config?.coursesLabel ?: 'Courses' %>
<% def defaultTab = config?.newsfeedEnabled ? 'newsfeed' : 'browse' %>

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
            <g:if test="${config?.newsfeedEnabled}">
                <button class="tab-btn ${defaultTab == 'newsfeed' ? 'tab-active' : 'text-stone-500'} px-4 py-3 text-sm font-medium whitespace-nowrap min-h-[44px]"
                        hx-get="/universal/showView"
                        hx-vals='{"template": "newsfeed/feed", "data[user]": "currentUser", "data[posts]": "list:WallPost"}'
                        hx-target="#content"
                        hx-swap="innerHTML"
                        onclick="setActiveTab(this)">
                    Newsfeed
                </button>
            </g:if>
            <button class="tab-btn ${defaultTab == 'browse' ? 'tab-active' : 'text-stone-500'} px-4 py-3 text-sm font-medium whitespace-nowrap min-h-[44px]"
                    hx-get="/universal/showView"
                    hx-vals='{"template": "courses/browse", "data[courses]": "list:Course", "data[user]": "currentUser", "data[config]": "get:AppConfig:configId", "configId": "${config?.id}"}'
                    hx-target="#content"
                    hx-swap="innerHTML"
                    onclick="setActiveTab(this)">
                Browse
            </button>
            <button class="tab-btn px-4 py-3 text-sm font-medium whitespace-nowrap text-stone-500 min-h-[44px]"
                    hx-get="/universal/showView"
                    hx-vals='{"template": "courses/myCourses", "data[user]": "currentUser", "data[enrollments]": "filter:CourseEnrollment:user.id=currentUserId"}'
                    hx-target="#content"
                    hx-swap="innerHTML"
                    onclick="setActiveTab(this)">
                My ${label}
            </button>
            <sec:ifAnyGranted roles='ROLE_ADMIN,ROLE_TEACHER'>
                <button class="tab-btn px-4 py-3 text-sm font-medium whitespace-nowrap text-stone-500 min-h-[44px]"
                        hx-get="/universal/showView"
                        hx-vals='{"template": "lessons/manage", "data[user]": "currentUser", "data[myCourses]": "filter:Course:creator.id=currentUserId"}'
                        hx-target="#content"
                        hx-swap="innerHTML"
                        onclick="setActiveTab(this)">
                    Create
                </button>
            </sec:ifAnyGranted>
            <button class="tab-btn px-4 py-3 text-sm font-medium whitespace-nowrap text-stone-500 min-h-[44px]"
                    hx-get="/universal/showView"
                    hx-vals='{"template": "profile/view", "data[user]": "currentUser", "data[badges]": "filter:UserBadge:user.id=currentUserId", "data[enrollmentCount]": "filterCount:CourseEnrollment:user.id=currentUserId", "data[config]": "get:AppConfig:configId", "configId": "${config?.id}"}'
                    hx-target="#content"
                    hx-swap="innerHTML"
                    onclick="setActiveTab(this)">
                Profile
            </button>
            <sec:ifAnyGranted roles='ROLE_ADMIN'>
                <button class="tab-btn px-4 py-3 text-sm font-medium whitespace-nowrap text-stone-500 min-h-[44px]"
                        hx-get="/universal/showView"
                        hx-vals='{"template": "settings/index", "data[user]": "currentUser"}'
                        hx-target="#content"
                        hx-swap="innerHTML"
                        onclick="setActiveTab(this)">
                    Settings
                </button>
            </sec:ifAnyGranted>
        </nav>
    </div>
</header>

<!-- Content area -->
<div class="max-w-6xl mx-auto px-4">
    <g:if test="${config?.newsfeedEnabled}">
        <div id="content" class="py-4"
             hx-get="/universal/showView"
             hx-vals='{"template": "newsfeed/feed", "data[user]": "currentUser", "data[posts]": "list:WallPost"}'
             hx-trigger="load"
             hx-swap="innerHTML">
        </div>
    </g:if>
    <g:else>
        <div id="content" class="py-4"
             hx-get="/universal/showView"
             hx-vals='{"template": "courses/browse", "data[courses]": "list:Course", "data[user]": "currentUser", "data[config]": "get:AppConfig:configId", "configId": "${config?.id}"}'
             hx-trigger="load"
             hx-swap="innerHTML">
        </div>
    </g:else>
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
