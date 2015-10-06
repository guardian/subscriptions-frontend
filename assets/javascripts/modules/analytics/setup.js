define([
    'utils/cookie',
    'modules/analytics/ga',
    'modules/analytics/omniture',
    'modules/analytics/remarketing',
    'modules/analytics/krux',
    'modules/analytics/affectv'
], function (
    cookie,
    ga,
    omniture,
    remarketing,
    krux,
    affectv
) {
    'use strict';

    function init() {
        var analyticsEnabled = (
            window.guardian.analyticsEnabled &&
            !navigator.doNotTrack &&
            !cookie.getCookie('ANALYTICS_OFF_KEY')
        );

        if (analyticsEnabled) {
            ga.init();
            omniture.init();

            if (!window.guardian.isDev) {
                remarketing.init();
                krux.init();
                affectv.init();
            }
        }
    }

    return {
        init: init
    };
});
