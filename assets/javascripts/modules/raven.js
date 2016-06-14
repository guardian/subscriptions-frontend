define(['lodash/object/pick', 'utils/user', 'raven-js'], function (pick, userUtil, Raven) {
    'use strict';

    function getTags(buildNumber, user) {
        return pick({
          build_number: buildNumber,
          userIdentityId: user ? user.id : undefined
        }, function(val) {
          return val !== undefined;
        });
    }

    function init(dsn) {
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
            shouldSendCallback: function(data) {
                if(window.guardian.isDev && window.console && window.console.warn) {
                    console.warn('Raven captured error: ', data);
                }
                return !window.guardian.isDev;
            }
        }).install();
    }

    return {
        init: init,
        getTags: getTags,
        Raven: Raven
    };
});
