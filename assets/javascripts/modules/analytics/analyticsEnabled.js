define([
    'utils/cookie'
], function (cookie) {
    'use strict';

    /*
    Re: https://bugzilla.mozilla.org/show_bug.cgi?id=1023920#c2

    The landscape at the moment is:

        On navigator [Firefox, Chrome, Opera]
        On window [IE, Safari]
    */
    var isDNT = navigator.doNotTrack == '1' || window.doNotTrack == '1';

    var analyticsEnabled = (
        window.guardian.analyticsEnabled &&
        !isDNT &&
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
