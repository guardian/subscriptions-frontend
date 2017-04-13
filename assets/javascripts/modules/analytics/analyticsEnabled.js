define([
    'utils/cookie'
], function (cookie) {
    'use strict';

    var analyticsEnabled = (
        window.guardian.analyticsEnabled &&
        !cookie.getCookie('ANALYTICS_OFF_KEY') &&
        !window.guardian.isDev
    );

    return function (cb) {
        return function () {
            if (analyticsEnabled) {
                return cb();
            }
        };
    };
});
