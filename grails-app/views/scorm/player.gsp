<%@ page import="grails.converters.JSON" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>${course.shortTitle} - ${config?.siteTitle ?: 'Korean School House'}</title>
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
        <a href="/?view=mycourses"
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

        var saveTimer = null;
        var saving = false;          // a POST is currently in flight
        var pendingAfterSave = false; // state changed while in flight -> save once more
        function persist() {
            if (saveTimer) { clearTimeout(saveTimer); saveTimer = null; }
            // Never overlap saves. Two concurrent POSTs load the same rows at the
            // same version; the second UPDATE matches 0 rows and Hibernate throws
            // a StaleStateException. Serialize to exactly one in-flight request and
            // coalesce anything that arrives while it's running.
            if (saving) { pendingAfterSave = true; return; }
            saving = true;
            var snapshot = JSON.stringify(cmi);
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '/api/scorm/${course.id}/cmi', true);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.onreadystatechange = function() {
                if (xhr.readyState === 4) {
                    saving = false;
                    if (pendingAfterSave) { pendingAfterSave = false; persist(); }
                }
            };
            xhr.send(snapshot);
        }
        // Coalesce SetValue bursts into a single save 10s after the last set.
        // The SCO normally calls Commit at meaningful checkpoints — this is a
        // safety net for SCOs that only commit on exit. Long debounce keeps
        // server load close to old behavior (~few saves per session, not per second).
        function scheduleSave() {
            if (saveTimer) return;
            saveTimer = setTimeout(function() { saveTimer = null; persist(); }, 10000);
        }

        function updateStatus() {
            // SCORM 1.2 uses cmi.core.lesson_status; SCORM 2004 splits into completion_status + success_status.
            var status = cmi['cmi.completion_status'] || cmi['cmi.core.lesson_status'] || 'not attempted';
            if (statusEl) statusEl.textContent = status.charAt(0).toUpperCase() + status.slice(1);
        }

        // Shared core. Used by both SCORM 1.2 (window.API) and SCORM 2004 (window.API_1484_11).
        // The SCO walks up parent windows looking for whichever name matches its version.
        function coreInit()        { if (initialized) return 'true'; initialized = true; finished = false; updateStatus(); if (statusEl) statusEl.textContent = 'In progress'; return 'true'; }
        function coreFinish()      { if (!initialized || finished) return 'true'; finished = true; initialized = false; persist(); updateStatus(); return 'true'; }
        function coreGetValue(k)   { var v = cmi[k]; return (v !== undefined && v !== null) ? String(v) : ''; }
        function coreSetValue(k,v) { cmi[k] = v; if (k === 'cmi.core.lesson_status' || k === 'cmi.completion_status') updateStatus(); scheduleSave(); return 'true'; }
        function coreCommit()      { persist(); return 'true'; }

        // SCORM 1.2 — methods prefixed with LMS
        window.API = {
            LMSInitialize:      coreInit,
            LMSFinish:          coreFinish,
            LMSGetValue:        coreGetValue,
            LMSSetValue:        coreSetValue,
            LMSCommit:          coreCommit,
            LMSGetLastError:    function()  { return '0'; },
            LMSGetErrorString:  function(c) { return 'No error'; },
            LMSGetDiagnostic:   function(c) { return ''; }
        };

        // SCORM 2004 — same operations, no LMS prefix, distinct window key.
        window.API_1484_11 = {
            Initialize:      coreInit,
            Terminate:       coreFinish,
            GetValue:        coreGetValue,
            SetValue:        coreSetValue,
            Commit:          coreCommit,
            GetLastError:    function()  { return '0'; },
            GetErrorString:  function(c) { return 'No error'; },
            GetDiagnostic:   function(c) { return ''; }
        };

        // Persist on page unload in case SCO doesn't call LMSFinish.
        // Use sendBeacon: it's guaranteed to deliver during unload (a normal async
        // XHR is often killed mid-flight) and it sidesteps the in-flight guard,
        // which can't complete once the page is tearing down.
        window.addEventListener('beforeunload', function() {
            if (initialized && !finished) {
                if (navigator.sendBeacon) {
                    navigator.sendBeacon('/api/scorm/${course.id}/cmi',
                        new Blob([JSON.stringify(cmi)], { type: 'application/json' }));
                } else {
                    persist();
                }
            }
        });
    })();
</script>

</body>
</html>
