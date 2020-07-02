define(['modules/analytics/ga',
        'modules/analytics/ophan',
        'modules/analytics/thirdPartyTracking'
], function (ga,
             ophan,
             thirdPartyTracking) {
    'use strict';

    function init() {
        ophan.init()
            .catch(console.log)
            // GA prefers Ophan to have bootstrapped and set window.guardian state, so load it after Ophan has loaded
            .finally(ga.init);

        if (thirdPartyTracking.thirdPartyTrackingEnabled()){
            // As this site is mostly deprecated, we've removed all 3rd party trackers.
        }
    }

    return {
        init: init
    };
});
