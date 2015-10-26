define([
    'utils/cookie'
], function (cookie) {
    'use strict';

    var analyticsEnabled = (
        window.guardian.analyticsEnabled &&
        !navigator.doNotTrack &&
        !cookie.getCookie('ANALYTICS_OFF_KEY')
    );

    return function(cb){
        return function() {
            if (analyticsEnabled) {
                return cb()
            }
        }
    }
});
