/*global Raven, s_gi */
define(['modules/checkout/omniture'], function (checkoutOmniture) {
    'use strict';

    function onSuccess() {
        window.s_account = 'guardiangu-subscribe,guardiangu-network';
        var s = s_gi('guardiangu-network');
        var s_code;

        s.pageName = document.title;
        s.channel = 'Subscriber';
        s.prop3 = 'GU.co.uk';
        s.prop5 = 'Subscriber';
        s.prop11 = 'Subscriber';
        s.hier2 = 'GU/News/Subscriber/';
        s.prop14 = '24.0';
        s.prop19 = 'Subscriber';
        s.eVar19 = 'D=c19';
        s.prop42 = 'Subscriber';
        s.prop47 = 'UK';
        s_code = s.t();

        checkoutOmniture.init(s);

        if (s_code) {
            /*jslint evil: true */
            document.write(s_code);
        }
    }

    function init() {
        require(['js!omniture']).then(onSuccess, function(err) {
            Raven.captureException(err);
        });
    }

    return {
        init: init
    };
});
