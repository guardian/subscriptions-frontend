define([], function () {
    'use strict';

    /*
    Re: https://bugzilla.mozilla.org/show_bug.cgi?id=1023920#c2

    The landscape at the moment is:

        On navigator [Firefox, Chrome, Opera]
        On window [IE, Safari]
    */
    var isDNT = navigator.doNotTrack == '1' || window.doNotTrack == '1'; // == not === in case some random browser uses Number(1).

    return function (cb){
        return function () {
            if (!isDNT) {
                return cb();
            }
        };
    };
});
