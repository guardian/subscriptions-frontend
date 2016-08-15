define(['modules/analytics/ga',
        'modules/analytics/omniture',
        'modules/analytics/ophan',
        'modules/analytics/remarketing',
        'modules/analytics/krux',
        'modules/analytics/affectv',
        'modules/analytics/snowplow'
], function (ga,
             omniture,
             ophan,
             remarketing,
             krux,
             affectv,
             snowplow) {
    'use strict';

    function init() {
        snowplow.init();
        ophan.init();
        // ga prefers ophan to have bootstrapped and set window.guardian state etc
        ophan.loaded.then(ga.init, ga.init);
        omniture.init();

        if (!window.guardian.isDev) {
            remarketing.init();
            krux.init();
            affectv.init();
        }
    }

    return {
        init: init
    };
});
