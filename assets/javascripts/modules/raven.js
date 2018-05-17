define(['raven-js'], function (Raven) {
    'use strict';

    function init(dsn) {
        if (!guardian.isDev) {
            Raven.config(dsn, {
                whitelistUrls: [
                    /subscribe\.theguardian\.com\/assets/,
                    /sub\.thegulocal\.com/,
                    /localhost/
                ],
                tags: { build_number: window.guardian.buildNumber },
                ignoreErrors: [
                    /duplicate define: jquery/
                ],
                shouldSendCallback: function (data) {
                    if (window.guardian.isDev && window.console && window.console.warn) {
                        console.warn('Raven captured error: ', data);
                    }
                    return !window.guardian.isDev;
                },
                release: guardian.buildNumber
            }).install();
        }
    }

    return {
        init: init,
        Raven: Raven
    };
})
    ;
