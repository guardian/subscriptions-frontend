/*global guardian:true */
define([
    'utils/cookie',
    'modules/analytics/ga',
    'modules/analytics/omniture',
    'modules/analytics/krux',
    'modules/analytics/affectv'
], function (
    cookie,
    ga,
    omniture,
    krux,
    affectv
) {
    'use strict';

    function init() {

        var analyticsEnabled = (
            guardian.analyticsEnabled &&
            !navigator.doNotTrack &&
            !cookie.getCookie('ANALYTICS_OFF_KEY')
        );

        if (analyticsEnabled) {
            ga.init();
            omniture.init();
            krux.init();
            affectv.init();
        }

    }

    return {
        init: init
    };
});
