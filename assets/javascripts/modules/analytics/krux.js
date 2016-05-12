define(['modules/analytics/analyticsEnabled', 'modules/raven'], function(analyticsEnabled, raven) {
    'use strict';

    var KRUX_ID = 'Jglpp88U';

    function init() {
        require(['js!https://cdn.krxd.net/controltag?confid=' + KRUX_ID]).then(null, function(err) {
            raven.Raven.captureException(err);
        });
    }

    return {
        init: analyticsEnabled(init)
    };
});
