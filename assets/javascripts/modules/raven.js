define(['utils/user', 'raven-js'], function (userUtil, Raven) {
    'use strict';

    function getTags(buildNumber, user) {
        if (user && user.id) {
            return {
                build_number: buildNumber,
                userIdentityId: user.id
            }
        }
        return {
            build_number: buildNumber
        }
    }

    function init(dsn) {
        if (!guardian.isDev) {
            Raven.config(dsn, {
                whitelistUrls: [
                    /subscribe\.theguardian\.com\/assets/,
                    /sub\.thegulocal\.com/,
                    /localhost/
                ],
                tags: getTags(
                    window.guardian.buildNumber,
                    userUtil.getUserFromCookie()
                ),
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
        getTags: getTags,
        Raven: Raven
    };
})
    ;
