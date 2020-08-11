define([
    'modules/analytics/ga',
    'modules/analytics/ophan',
    'modules/analytics/thirdPartyTracking'
], function (
    ga,
    ophan,
    thirdPartyTracking
) {
    'use strict';

    function init() {
        ophan.init();

        /**
         * GA prefers Ophan to have bootstrapped and set window.guardian state,
         * so load it after Ophan has loaded if thirdPartyTrackingEnabled.
        */
        ophan.loaded.finally(() => {
            thirdPartyTracking.thirdPartyTrackingEnabled().then(thirdPartyTrackingEnabled => {
                if (thirdPartyTrackingEnabled) {
                    ga.init();
                }
            });
        });
    }

    return {
        init: init
    };
});
