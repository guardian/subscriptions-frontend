define(['raven'], function (raven) {
    'use strict';

    function init(dsn) {
        raven.config(dsn, {
            whitelistUrls: [
                /subscribe\.theguardian\.com\/assets/,
                /sub\.thegulocal\.com/,
                /localhost/
            ],
            tags: {
                build_number: guardian.buildNumber
            },
            ignoreErrors: [
                /duplicate define: jquery/
            ],
            shouldSendCallback: function(data) {
                if(window.guardian.isDev && window.console && window.console.warn) {
                    console.warn('Raven captured error: ', data);
                }
                return !window.guardian.isDev;
            }
        }).install();
    }

    return {
        init: init
    };
});
