/*global s_gi, guardian */
define([
    '$',
    'bean',
    'modules/analytics/analyticsEnabled',
    'modules/raven'
], function ($, bean, analyticsEnabled, raven) {
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
            productData = guardian.pageInfo.productData;

        s.pageName = pageInfo.name;
        s.channel = pageInfo.channel;
        s.prop19 = pageInfo.product;
        s.prop17 = pageInfo.slug;

        if (pageInfo.billingCountry && pageInfo.billingCurrency) {
            s.eVar16 = 'billingCountry:' + pageInfo.billingCountry + ',billingCurrency:' + pageInfo.billingCurrency;
        }

        if (productData) {
            s.products = [
                productData.source,
                productData.type,
                productData.frequency,
                productData.amount,
                productData.eventName
            ].join(';');
        }
    }

    function triggerPageLoadEvent() {
        omniture = omniture || init();
        omniture.then(function () {
            setTracker(s);
            var s_code = s.t();
            setTracker(s);
            if (s_code) {
                document.write(s_code);
            }
        });
    }

    function onSuccess() {
        window.s_account = 'guardiangu-subscribe,guardiangu-network';
        s = s_gi('guardiangu-network');
        if (!guardian.ignorePageLoadTracking) {
            triggerPageLoadEvent();
        }
        bindLinkTracking();
    }

    function init() {
        omniture = omniture || require(['js!omniture'])
                .then(onSuccess, function (err) {
                    raven.Raven.captureException(err);
                });
        return omniture;
    }

    return {
        init: analyticsEnabled(init),
        triggerPageLoadEvent: analyticsEnabled(triggerPageLoadEvent)
    };
});
