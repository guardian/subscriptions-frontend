define([
    'modules/analytics/analyticsEnabled',
    'modules/raven'
],function(analyticsEnabled, raven) {
    'use strict';

    var ophanUrl = 'https://j.ophan.co.uk/membership.js';

    var API = {
        loaded: Promise.resolve(null),
        init: analyticsEnabled(
            function() {
                // curl is 'thenable', but not a true Promise, so needs wrapping
                API.loaded = Promise.resolve(curl(ophanUrl).then(null, function(err) {
                    raven.Raven.captureException(err);
                }));
            },
            function() {
                console.warn('Ophan not loaded due to analytics disabled');
            }
        )
    };

    return API;
});
