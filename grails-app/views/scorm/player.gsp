<%@ page import="grails.converters.JSON" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>${course.shortTitle} - Korean School House</title>
    <style>
        body { overflow: hidden; }
        #scorm-player { display: flex; flex-direction: column; height: 100vh; }
        #scorm-content { flex: 1; border: none; width: 100%; }
    </style>
</head>
<body>

<div id="scorm-player">
    <!-- Player header -->
    <div class="bg-white shadow-sm px-4 py-2 flex items-center gap-3 flex-shrink-0">
        <a href="/"
           class="flex items-center gap-2 text-sm text-stone-500 hover:text-stone-700 min-h-[44px] py-2">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
            Exit
        </a>
        <h1 class="font-semibold text-stone-800 text-sm truncate flex-1">${course.shortTitle}</h1>
        <span id="scorm-status" class="text-xs text-stone-400">Loading...</span>
    </div>

    <!-- SCORM content iframe -->
    <iframe id="scorm-content"
            src="/scorm/content/${course.id}/${course.scormLaunchUrl}"
            allowfullscreen></iframe>
</div>

<script>
    // SCORM 1.2 API - must be defined BEFORE iframe loads
    (function() {
        var cmi = ${raw((cmiData as JSON).toString())};
        var initialized = false;
        var finished = false;
        var statusEl = document.getElementById('scorm-status');

        function persist() {
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '/api/scorm/${course.id}/cmi', true);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.send(JSON.stringify(cmi));
        }

        function updateStatus() {
            var status = cmi['cmi.core.lesson_status'] || 'not attempted';
            if (statusEl) statusEl.textContent = status.charAt(0).toUpperCase() + status.slice(1);
        }

        window.API = {
            LMSInitialize: function(param) {
                if (initialized) return 'true';
                initialized = true;
                finished = false;
                updateStatus();
                if (statusEl) statusEl.textContent = 'In progress';
                return 'true';
            },
            LMSFinish: function(param) {
                if (!initialized || finished) return 'true';
                finished = true;
                initialized = false;
                persist();
                updateStatus();
                return 'true';
            },
            LMSGetValue: function(key) {
                var val = cmi[key];
                return (val !== undefined && val !== null) ? String(val) : '';
            },
            LMSSetValue: function(key, value) {
                cmi[key] = value;
                if (key === 'cmi.core.lesson_status') {
                    updateStatus();
                }
                return 'true';
            },
            LMSCommit: function(param) {
                persist();
                return 'true';
            },
            LMSGetLastError: function() { return '0'; },
            LMSGetErrorString: function(code) { return 'No error'; },
            LMSGetDiagnostic: function(code) { return ''; }
        };

        // Persist on page unload in case SCO doesn't call LMSFinish
        window.addEventListener('beforeunload', function() {
            if (initialized && !finished) {
                persist();
            }
        });
    })();
</script>

</body>
</html>
