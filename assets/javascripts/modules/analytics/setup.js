define(['modules/analytics/ga',
        'modules/analytics/ophan',
        'modules/analytics/remarketing',
        'modules/analytics/krux',
        'modules/analytics/affectv'
], function (ga,
             ophan,
             remarketing,
             krux,
             affectv) {
    'use strict';

    function init() {
        ophan.init();
        // ga prefers ophan to have bootstrapped and set window.guardian state etc
        ophan.loaded.then(ga.init, ga.init);
        remarketing.init();
        krux.init();
        affectv.init();
    }

    return {
        init: init
    };
});
