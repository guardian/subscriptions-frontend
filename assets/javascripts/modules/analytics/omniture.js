/*global Raven, s_gi, guardian */
define([
    '$',
    'bean'
], function ($, bean) {
    'use strict';

    var omniture, s;

    function bindLinkTracking() {
        $('a[data-link-tracking]').each(function (domElem) {
            bean.on(domElem, 'click', function () {
                trackLink($(this).attr('href'));
            });
        });
    }

    function trackLink(link) {
        omniture = omniture || init();
        omniture.then(function () {
            s.tl(true, 'o', link);
        });
    }

    function setTracker(s) {
        var pageInfo = guardian.pageInfo,
            productData = guardian.productData;

        s.pageName = pageInfo.name;
        s.channel = pageInfo.channel;
        s.prop3 = pageInfo.publication;
        s.prop5 = pageInfo.commissioningDesk;
        s.prop11 = 'Subscriber';
        s.prop14 = '24.0';
        s.prop19 = pageInfo.product;
        s.eVar19 = 'D=c19';
        s.prop42 = 'Subscriber';
        s.prop47 = pageInfo.edition;

        if (productData) {
            s.products = [
                productData.source,
                productData.type,
                productData.frequency,
                productData.amount,
                productData.eventName
            ].join(';');
        }
        if (guardian.slug) {
            s.prop17 = guardian.slug;
        }
    }

    function triggerPageLoadEvent() {
        omniture = omniture || init();
        omniture.then(function () {
            setTracker(s);
            var s_code = s.t();
            setTracker(s);
            if (s_code) {
                /*jslint evil: true */
                document.write(s_code);
            }
        });
    }

    function onSuccess() {
        window.s_account = 'guardiangu-subscribe,guardiangu-network';
        s = s_gi('guardiangu-network');
        if(!document.querySelector('main[data-no-page-load-tracking]')) {
            triggerPageLoadEvent();
        }
        bindLinkTracking();
    }

    function init() {
        omniture = omniture || require(['js!omniture'])
                .then(onSuccess, function (err) {
                    Raven.captureException(err);
                });
        return omniture;
    }

    return {
        init: init,
        triggerPageLoadEvent: triggerPageLoadEvent
    };
});
