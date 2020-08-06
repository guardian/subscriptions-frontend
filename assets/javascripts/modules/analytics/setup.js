define(['modules/analytics/ga',
        'modules/analytics/ophan',
        // 'modules/analytics/thirdPartyTracking'
], function (ga,
             ophan,
            //  thirdPartyTracking
             ) {
    'use strict';

    function init() {
        ophan.init();

        // GA prefers Ophan to have bootstrapped and set window.guardian state, so load it after Ophan has loaded
        ophan.loaded.finally(ga.init);

        // thirdPartyTracking.thirdPartyTrackingEnabled().then(thirdPartyTrackingEnabled => {
            // As this site is mostly deprecated, we've removed all 3rd party trackers.
        // });
    }

    return {
        init: init
    };
});
