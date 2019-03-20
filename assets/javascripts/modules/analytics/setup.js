define(['modules/analytics/ga',
        'modules/analytics/ophan',
        'modules/analytics/remarketing',
        'modules/analytics/krux',
        'modules/analytics/facebook',
        'modules/analytics/appnexus',
        'modules/analytics/awin',
        'modules/analytics/thirdPartyTracking'
], function (ga,
             ophan,
             remarketing,
             krux,
             facebook,
             appnexus,
             awin,
             thirdPartyTracking) {
    'use strict';

    function init() {
        ophan.init();
        // ga prefers ophan to have bootstrapped and set window.guardian state etc
        ophan.loaded.then(ga.init, ga.init);

        if (thirdPartyTracking.thirdPartyTrackingEnabled()){
            remarketing.init();
            krux.init();
            facebook.init();
            appnexus.init();
            awin.init();
        }
    }

    return {
        init: init
    };
});
