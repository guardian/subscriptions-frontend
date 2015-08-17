/*global Raven, s_gi */
define([
    '$',
    'bean'
], function ($, bean) {
    'use strict';

    var dataTrackingClickableElements = ['a', 'button', 'input'],
        omniture;

    function sendEvent(prop17, products) {
        if (!omniture) {
            Raven.captureException('Omniture client not found');
            return;
        }
        if (prop17) {
            omniture.prop17 = prop17;
        }
        if (products) {
            omniture.products = products;
        }
        if (prop17 || products) {
            omniture.t();
        }
    }

    function bindDataTracking() {
        dataTrackingClickableElements.forEach(function (elem) {
            $(elem + '[data-tracking]').each(function (domElem) {
                var prop17 = domElem.getAttribute('data-tracking-prop17'),
                    products = domElem.getAttribute('data-tracking-products');

                bean.on(domElem, 'click', function () {
                    sendEvent(prop17, products);
                });
            });
        });
    }

    function onSuccess() {
        window.s_account = 'guardiangu-subscribe,guardiangu-network';
        var s = s_gi('guardiangu-network'),
            s_code;

        omniture = s;

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

        bindDataTracking();

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
        init: init,
        sendEvent: sendEvent
    };
});
