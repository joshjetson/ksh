<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>${config?.siteTitle ?: 'Korean School House'}</title>
</head>
<body>

<% def label = config?.coursesLabel ?: 'Courses' %>
<% def defaultTab = config?.newsfeedEnabled ? 'community' : 'browse' %>
<%-- Landing view: arriving from a lesson (?view=mycourses, e.g. the player's Exit)
     opens My Courses; otherwise the configured default. --%>
<% def landing = (params.view == 'mycourses') ? 'mycourses' : defaultTab %>
<%
    // Every navigation destination declared ONCE — rendered into the desktop sidebar,
    // the mobile bottom tab bar, and the mobile "More" sheet via _navLink variants.
    def V = [
        homeAdmin:   '{"template": "dashboard/admin"}',
        homeStudent: '{"template": "dashboard/overview", "data[stats]": "service:dashboardService:studentStats", "data[pinned]": "filter:Announcement:pinned=true", "data[myClasses]": "filter:ClassroomMembership:term.id=activeTerm", "data[myGrades]": "filter:Grade:term.id=activeTerm"}',
        community:   '{"template": "community/wall", "data[user]": "currentUser", "data[wall]": "service:communityService:generalWall"}',
        myWall:      '{"template": "community/myWall", "data[user]": "currentUser", "data[walls]": "service:communityService:myWall"}',
        searchPosts: '{"template": "community/search", "data[user]": "currentUser", "data[res]": "service:communityService:searchPosts"}',
        browse:      '{"template": "courses/browse", "data[courses]": "list:Course", "data[user]": "currentUser", "data[config]": "get:AppConfig:configId", "configId": "' + (config?.id ?: '') + '"}',
        mycourses:   '{"template": "courses/myCourses", "data[user]": "currentUser", "data[enrollments]": "filter:CourseEnrollment:user.id=currentUserId"}',
        create:      '{"template": "lessons/manage", "data[user]": "currentUser", "data[myCourses]": "filter:Course:creator.id=currentUserId"}',
        calendar:    '{"template": "calendar/month", "data[cal]": "service:scheduleService:monthView", "data[user]": "currentUser"}',
        messages:    '{"template": "messaging/home", "data[list]": "service:messageService:channelList", "data[user]": "currentUser"}',
        announcements: '{"template": "announcements/list", "data[user]": "currentUser", "data[items]": "list:Announcement"}',
        files:       '{"template": "files/list", "data[user]": "currentUser", "data[items]": "list:FileResource"}',
        media:       '{"template": "media/list", "data[user]": "currentUser", "data[items]": "list:MediaPost"}',
        profile:     '{"template": "profile/view", "data[user]": "currentUser", "data[badges]": "filter:UserBadge:user.id=currentUserId", "data[enrollmentCount]": "filterCount:CourseEnrollment:user.id=currentUserId", "data[config]": "get:AppConfig:configId", "configId": "' + (config?.id ?: '') + '"}',
        enrollments: '{"template": "enrollments/manage", "data[enrollments]": "list:CourseEnrollment"}',
        requests:    '{"template": "requests/manage", "data[requests]": "filter:EnrollmentChangeRequest:status=PENDING"}',
        discounts:   '{"template": "discounts/list", "data[codes]": "list:DiscountCode"}',
        settings:    '{"template": "settings/index", "data[user]": "currentUser"}',
        classrooms:  '{"template": "classrooms/list", "data[items]": "list:Classroom", "data[user]": "currentUser"}',
        myClassroom: '{"template": "classrooms/myClassrooms", "data[assignments]": "list:ClassroomStaff", "data[user]": "currentUser"}',
        terms:       '{"template": "terms/list", "data[items]": "list:Term", "data[user]": "currentUser"}',
        attendance:  '{"template": "attendance/record", "data[classrooms]": "list:Classroom", "data[user]": "currentUser"}',
    ]
    def firstPaint = (landing == 'mycourses') ? V.mycourses : (landing == 'community' ? V.community : V.browse)
%>

<div class="min-h-screen md:flex">

    <%-- ── Desktop sidebar ─────────────────────────────────────────────── --%>
    <aside class="hidden md:flex md:flex-col md:w-60 md:shrink-0 bg-white border-r border-cream-200 md:h-screen md:sticky md:top-0">
        <div class="px-4 h-14 flex items-center gap-2 border-b border-cream-200">
            <span class="text-xl font-bold text-rose-700">${config?.logoText ?: '한'}</span>
            <span class="font-bold text-stone-800 truncate">${config?.siteTitle ?: 'Korean School House'}</span>
        </div>
        <nav class="flex-1 overflow-y-auto p-3 space-y-1">
            <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
                <g:render template="/universal/components/navLink" model="[key:'home', label:'Dashboard', icon:'home', vals:V.homeAdmin, variant:'side']"/>
            </sec:ifAnyGranted>
            <sec:ifNotGranted roles="ROLE_ADMIN,ROLE_TEACHER">
                <g:render template="/universal/components/navLink" model="[key:'home', label:'Dashboard', icon:'home', vals:V.homeStudent, variant:'side']"/>
            </sec:ifNotGranted>
            <g:if test="${config?.newsfeedEnabled}">
                <g:render template="/universal/components/navLink" model="[key:'community', label:'Community', icon:'heart', vals:V.community, variant:'side', active:(landing == 'community')]"/>
                <g:render template="/universal/components/navLink" model="[key:'myWall', label:'My Wall', icon:'bookmark', vals:V.myWall, variant:'side']"/>
                <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
                    <g:render template="/universal/components/navLink" model="[key:'searchPosts', label:'Search Posts', icon:'search', vals:V.searchPosts, variant:'side']"/>
                </sec:ifAnyGranted>
            </g:if>
            <g:render template="/universal/components/navLink" model="[key:'browse', label:'Browse', icon:'grid', vals:V.browse, variant:'side', active:(landing == 'browse')]"/>
            <g:render template="/universal/components/navLink" model="[key:'mycourses', label:'My ' + label, icon:'book', vals:V.mycourses, variant:'side', active:(landing == 'mycourses')]"/>
            <g:render template="/universal/components/navLink" model="[key:'calendar', label:'Calendar', icon:'calendar', vals:V.calendar, variant:'side']"/>
            <g:render template="/universal/components/navLink" model="[key:'announcements', label:'Announcements', icon:'bullhorn', vals:V.announcements, variant:'side']"/>
            <g:render template="/universal/components/navLink" model="[key:'files', label:'Files', icon:'doc', vals:V.files, variant:'side']"/>
            <g:render template="/universal/components/navLink" model="[key:'media', label:'Media', icon:'photo', vals:V.media, variant:'side']"/>
            <g:render template="/universal/components/navLink" model="[key:'messages', label:'Messages', icon:'chat', vals:V.messages, variant:'side']"/>
            <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
                <g:render template="/universal/components/navLink" model="[key:'create', label:'Create', icon:'plus', vals:V.create, variant:'side']"/>
                <p class="px-3 pt-4 pb-1 text-[11px] font-semibold text-stone-400 uppercase tracking-wide">School</p>
                <g:render template="/universal/components/navLink" model="[key:'myClassroom', label:'My Classroom', icon:'bookmark', vals:V.myClassroom, variant:'side']"/>
                <g:render template="/universal/components/navLink" model="[key:'attendance', label:'Attendance', icon:'clipboard', vals:V.attendance, variant:'side']"/>
            </sec:ifAnyGranted>
            <g:render template="/universal/components/navLink" model="[key:'profile', label:'Profile', icon:'account', vals:V.profile, variant:'side']"/>

            <sec:ifAnyGranted roles="ROLE_ADMIN">
                <p class="px-3 pt-4 pb-1 text-[11px] font-semibold text-stone-400 uppercase tracking-wide">Admin</p>
                <g:render template="/universal/components/navLink" model="[key:'classrooms', label:'Classrooms', icon:'grid', vals:V.classrooms, variant:'side']"/>
                <g:render template="/universal/components/navLink" model="[key:'terms', label:'Terms', icon:'calendar', vals:V.terms, variant:'side']"/>
                <g:render template="/universal/components/navLink" model="[key:'enrollments', label:'Enrollments', icon:'clipboard', vals:V.enrollments, variant:'side']"/>
                <g:render template="/universal/components/navLink" model="[key:'requests', label:'Requests', icon:'inbox', vals:V.requests, variant:'side']"/>
                <g:render template="/universal/components/navLink" model="[key:'discounts', label:'Discounts', icon:'tag', vals:V.discounts, variant:'side']"/>
            </sec:ifAnyGranted>
            <div class="pt-2 mt-2 border-t border-cream-200">
                <g:render template="/universal/components/navLink" model="[key:'settings', label:'Settings', icon:'cog', vals:V.settings, variant:'side']"/>
            </div>
        </nav>
        <div class="p-3 border-t border-cream-200 flex items-center justify-between">
            <span class="text-xs text-stone-500 truncate"><sec:username/></span>
            <a href="/logout" class="text-xs font-medium text-stone-500 hover:text-rose-600">Sign out</a>
        </div>
    </aside>

    <%-- ── Main column ─────────────────────────────────────────────────── --%>
    <div class="flex-1 min-w-0 flex flex-col">
        <header class="md:hidden sticky top-0 z-20 bg-white border-b border-cream-200 h-14 flex items-center justify-between px-4">
            <a href="/" class="font-bold text-stone-800 flex items-center gap-2 truncate">
                <span class="text-xl text-rose-700">${config?.logoText ?: '한'}</span>
                <span class="truncate">${config?.siteTitle ?: 'Korean School House'}</span>
            </a>
            <a href="/logout" class="text-xs font-medium text-stone-500 hover:text-rose-600 shrink-0">Sign out</a>
        </header>

        <main class="flex-1">
            <%-- #content carries hx-vals (the landing template) for its first-paint load.
                 htmx leaks that into descendant requests via hx-vals inheritance, so any
                 descendant that doesn't set its own `template` (e.g. a chat composer)
                 would send the landing template. Forms that re-render set their own
                 template (overriding it); quiet posters explicitly clear it with
                 "template": "" instead. --%>
            <div id="content" class="max-w-6xl mx-auto w-full px-4 py-5 pb-24 md:pb-8"
                 hx-get="/universal/showView" hx-vals='${firstPaint}'
                 hx-trigger="load" hx-swap="innerHTML"></div>
        </main>
    </div>
</div>

<%-- ── Mobile bottom tab bar ───────────────────────────────────────────── --%>
<nav class="md:hidden fixed bottom-0 inset-x-0 z-30 bg-white/95 backdrop-blur border-t border-cream-200 flex pb-[env(safe-area-inset-bottom)]">
    <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
        <g:render template="/universal/components/navLink" model="[key:'home', label:'Home', icon:'home', vals:V.homeAdmin, variant:'tab']"/>
    </sec:ifAnyGranted>
    <sec:ifNotGranted roles="ROLE_ADMIN,ROLE_TEACHER">
        <g:render template="/universal/components/navLink" model="[key:'home', label:'Home', icon:'home', vals:V.homeStudent, variant:'tab']"/>
    </sec:ifNotGranted>
    <g:render template="/universal/components/navLink" model="[key:'browse', label:'Browse', icon:'grid', vals:V.browse, variant:'tab', active:(landing == 'browse')]"/>
    <g:render template="/universal/components/navLink" model="[key:'mycourses', label:'My ' + label, icon:'book', vals:V.mycourses, variant:'tab', active:(landing == 'mycourses')]"/>
    <g:render template="/universal/components/navLink" model="[key:'messages', label:'Messages', icon:'chat', vals:V.messages, variant:'tab']"/>
    <button type="button" onclick="openMore()" class="nav-tab flex-1 flex flex-col items-center justify-center gap-0.5 py-1.5 text-[11px] font-medium text-stone-500 min-h-[44px]">
        <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="M4 6h16M4 12h16M4 18h16"/></svg>
        <span>More</span>
    </button>
</nav>

<%-- ── Mobile "More" sheet ─────────────────────────────────────────────── --%>
<div id="more-overlay" class="hidden md:hidden fixed inset-0 z-40 bg-black/40" onclick="closeMore()"></div>
<div id="more-sheet" class="hidden md:hidden fixed bottom-0 inset-x-0 z-50 bg-white rounded-t-3xl p-4 pb-8 translate-y-full">
    <div class="mx-auto mb-3 h-1.5 w-10 rounded-full bg-cream-300"></div>
    <h3 class="text-sm font-semibold text-stone-400 mb-2 px-1">More</h3>
    <div class="space-y-1">
        <g:if test="${config?.newsfeedEnabled}">
            <g:render template="/universal/components/navLink" model="[key:'community', label:'Community', icon:'heart', vals:V.community, variant:'drawer', active:(landing == 'community')]"/>
            <g:render template="/universal/components/navLink" model="[key:'myWall', label:'My Wall', icon:'bookmark', vals:V.myWall, variant:'drawer']"/>
            <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
                <g:render template="/universal/components/navLink" model="[key:'searchPosts', label:'Search Posts', icon:'search', vals:V.searchPosts, variant:'drawer']"/>
            </sec:ifAnyGranted>
        </g:if>
        <g:render template="/universal/components/navLink" model="[key:'profile', label:'Profile', icon:'account', vals:V.profile, variant:'drawer']"/>
        <g:render template="/universal/components/navLink" model="[key:'calendar', label:'Calendar', icon:'calendar', vals:V.calendar, variant:'drawer']"/>
        <g:render template="/universal/components/navLink" model="[key:'announcements', label:'Announcements', icon:'bullhorn', vals:V.announcements, variant:'drawer']"/>
        <g:render template="/universal/components/navLink" model="[key:'files', label:'Files', icon:'doc', vals:V.files, variant:'drawer']"/>
        <g:render template="/universal/components/navLink" model="[key:'media', label:'Media', icon:'photo', vals:V.media, variant:'drawer']"/>
        <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
            <div class="border-t border-cream-200 my-2"></div>
            <g:render template="/universal/components/navLink" model="[key:'create', label:'Create', icon:'plus', vals:V.create, variant:'drawer']"/>
            <g:render template="/universal/components/navLink" model="[key:'myClassroom', label:'My Classroom', icon:'bookmark', vals:V.myClassroom, variant:'drawer']"/>
            <g:render template="/universal/components/navLink" model="[key:'attendance', label:'Attendance', icon:'clipboard', vals:V.attendance, variant:'drawer']"/>
        </sec:ifAnyGranted>
        <sec:ifAnyGranted roles="ROLE_ADMIN">
            <div class="border-t border-cream-200 my-2"></div>
            <g:render template="/universal/components/navLink" model="[key:'classrooms', label:'Classrooms', icon:'grid', vals:V.classrooms, variant:'drawer']"/>
            <g:render template="/universal/components/navLink" model="[key:'terms', label:'Terms', icon:'calendar', vals:V.terms, variant:'drawer']"/>
            <g:render template="/universal/components/navLink" model="[key:'enrollments', label:'Enrollments', icon:'clipboard', vals:V.enrollments, variant:'drawer']"/>
            <g:render template="/universal/components/navLink" model="[key:'requests', label:'Requests', icon:'inbox', vals:V.requests, variant:'drawer']"/>
            <g:render template="/universal/components/navLink" model="[key:'discounts', label:'Discounts', icon:'tag', vals:V.discounts, variant:'drawer']"/>
        </sec:ifAnyGranted>
        <div class="border-t border-cream-200 my-2"></div>
        <g:render template="/universal/components/navLink" model="[key:'settings', label:'Settings', icon:'cog', vals:V.settings, variant:'drawer']"/>
    </div>
</div>

<script>
    // Keep the same destination highlighted across sidebar + bottom bar + drawer.
    function kshNav(el) {
        var key = el.getAttribute('data-nav');
        document.querySelectorAll('[data-nav]').forEach(function (n) {
            n.classList.toggle('nav-active', n.getAttribute('data-nav') === key);
        });
        closeMore();
    }
    function openMore() {
        var o = document.getElementById('more-overlay'), s = document.getElementById('more-sheet');
        o.classList.remove('hidden'); s.classList.remove('hidden');
        requestAnimationFrame(function () { s.classList.remove('translate-y-full'); });
    }
    function closeMore() {
        var o = document.getElementById('more-overlay'), s = document.getElementById('more-sheet');
        if (s.classList.contains('hidden')) return;
        s.classList.add('translate-y-full');
        setTimeout(function () { o.classList.add('hidden'); s.classList.add('hidden'); }, 250);
    }
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeMore(); });
</script>

</body>
</html>
