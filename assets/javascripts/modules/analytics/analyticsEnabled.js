define([
    'utils/cookie'
], function (cookie) {
    'use strict';

    var analyticsEnabled = window.guardian.isDev ? window.guardian.analyticsEnabledInDev:
        window.guardian.analyticsEnabled && !cookie.getCookie('ANALYTICS_OFF_KEY');

    return function (cbEnabled, cbDisabled) {
        return function () {
            if (analyticsEnabled) {
                if (typeof cbEnabled === 'function') {
                    return cbEnabled();
                }
            } else {
                if (typeof cbDisabled === 'function') {
                    return cbDisabled();
                }
            }
        };
    };
});
