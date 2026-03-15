<!doctype html>
<html lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <title>
        <g:layoutTitle default="Korean School House"/>
    </title>
    <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no"/>
    <meta name="theme-color" content="#ffe4c4"/>

    <!-- CSRF token for HTMX POST requests -->
    <meta name="_csrf" content="${request.getAttribute('_csrf')?.token}" />
    <meta name="_csrf_header" content="${request.getAttribute('_csrf')?.headerName}" />

    <!-- PWA Meta Tags -->
    <meta name="mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="default">
    <meta name="apple-mobile-web-app-title" content="KSH">

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico"/>
    <asset:link rel="apple-touch-icon" href="apple-touch-icon.png"/>
    <asset:link rel="apple-touch-icon" href="apple-touch-icon-retina.png" sizes="152x152"/>
    <asset:link rel="manifest" href="manifest.json"/>

    <asset:stylesheet src="application.css"/>
    <style>
        .htmx-indicator { display: none; }
        .htmx-request .htmx-indicator { display: inline-block; }
        .htmx-request button { opacity: 0.6; cursor: not-allowed; }
    </style>

    <g:layoutHead/>
</head>

<body class="min-h-screen" style="background-color: #ffe4c4;">

    <g:layoutBody/>

    <asset:javascript src="application.js"/>

</body>
</html>
