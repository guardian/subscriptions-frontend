define([
    'modules/analytics/analyticsEnabled',
    'modules/analytics/dntEnabled',
    'modules/raven'
], function (analyticsEnabled, dntEnabled, raven) {
    'use strict';

    var KRUX_ID = 'Jglpp88U';

    function init() {
        curl(['js!https://cdn.krxd.net/controltag?confid=' + KRUX_ID]).then(null, function(err) {
            raven.Raven.captureException(err);
        });
    }

    return {
        init: analyticsEnabled(dntEnabled(init))
    };
});
