define([
    'modules/analytics/analyticsEnabled',
    'modules/raven'
],function(analyticsEnabled, raven) {
    'use strict';

    var ophanUrl = '//j.ophan.co.uk/membership.js';

    function init() {
        return curl(ophanUrl).then(null, function(err) {
            raven.Raven.captureException(err);
        });
    }

    return {
        init: analyticsEnabled(
            function() { return Promise.resolve(init()); }, // init is 'thenable', not a true Promise, so needs wrapping
            function() { return Promise.reject('Ophan not loaded due to analytics disabled') }
        )
    };
});
