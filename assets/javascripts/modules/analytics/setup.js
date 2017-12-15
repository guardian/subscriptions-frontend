define(['modules/analytics/ga',
        'modules/analytics/ophan',
        'modules/analytics/remarketing',
        'modules/analytics/krux',
        'modules/analytics/facebook',
        'modules/analytics/appnexus',
        'modules/analytics/affectv',
        'modules/analytics/awin'
], function (ga,
             ophan,
             remarketing,
             krux,
             facebook,
             appnexus,
             affectv,
             awin) {
    'use strict';

    function init() {
        ophan.init();
        // ga prefers ophan to have bootstrapped and set window.guardian state etc
        ophan.loaded.then(ga.init, ga.init);
        remarketing.init();
        krux.init();
        facebook.init();
        appnexus.init();
        affectv.init();
        awin.init();
    }

    return {
        init: init
    };
});
