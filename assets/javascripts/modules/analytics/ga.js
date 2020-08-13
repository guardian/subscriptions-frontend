/* global ga */
define(['modules/analytics/analyticsEnabled',
        'utils/cookie',
        'utils/user'
    ], function(analyticsEnabled, cookie, user) {
    'use strict';

    var _EVENT_QUEUE = [];

    function queryParam(key){
        return new RegExp(key + '=([^&]*)').exec(location.search);
    }

    function init() {
        var identitySignedIn = user.isLoggedIn();
        var identitySignedOut = !!cookie.getCookie('GU_SO') && !identitySignedIn;
        var ophanBrowserId = cookie.getCookie('bwid');
        var productData = guardian.pageInfo ? guardian.pageInfo.productData : {};
        var intcmp = queryParam('INTCMP');
        var isCustomerAgent = !!guardian.supplierCode;
        var camCodeBusinessUnit = queryParam('CMP_BUNIT');
        var camCodeTeam = queryParam('CMP_TU');
        var startTrialButton = queryParam('startTrialButton');
        var experience = guardian.experience;

        /* Google analytics snippet */
        /*eslint-disable */
        (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
            m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
        })(window,document,'script','//www.google-analytics.com/analytics.js','ga');
        /*eslint-enable */

        ga('create', {
            'trackingId': guardian.googleAnalytics.trackingId,
            'name': 'membershipPropertyTracker',
            'allowLinker': true,
            'cookieDomain': guardian.googleAnalytics.cookieDomain
        });

        if (guardian.optimizeEnabled) {
            ga('membershipPropertyTracker.require', 'GTM-NZGXNBL');
        }

        ga('membershipPropertyTracker.require', 'linkid');
        ga('membershipPropertyTracker.require', 'linker');
        ga('membershipPropertyTracker.linker:autoLink', ['eventbrite.co.uk'] );

        ga('membershipPropertyTracker.set', 'dimension1', identitySignedIn.toString());   // deprecated - now uses dimension7 via util/user
        ga('membershipPropertyTracker.set', 'dimension2', identitySignedOut.toString());  // deprecated - is logically equivalent to: dimension6 != "" and dimension7 === "false"
        (guardian.ophan) && ga('membershipPropertyTracker.set', 'dimension3', guardian.ophan.pageViewId); // ophanPageview Id
        (ophanBrowserId) && ga('membershipPropertyTracker.set', 'dimension4', ophanBrowserId); // ophanBrowserId
        ga('membershipPropertyTracker.set', 'dimension5', 'subscriptions');               // platform
        (identitySignedIn) && ga('membershipPropertyTracker.set', 'dimension6', user.getUserFromCookie().id); // identityId
        ga('membershipPropertyTracker.set', 'dimension7', identitySignedIn.toString());   // isLoggedOn

        if (productData) {
            if (productData.zuoraId) {
                ga('membershipPropertyTracker.set', 'dimension9', productData.zuoraId);   // zuoraId
            }
            if (productData.productPurchased) {
                ga('membershipPropertyTracker.set', 'dimension11', productData.productType + ' - ' + productData.productPurchased);  // productPurchased
            }
            if (productData.promoCode) {
                ga('membershipPropertyTracker.set', 'dimension19', productData.promoCode);  // promoCode
            }
        }

        if (intcmp && intcmp[1]) {
            ga('membershipPropertyTracker.set', 'dimension12', intcmp[1]);  // internalCampCode
        }

        if (startTrialButton && startTrialButton[1]) {
            ga('membershipPropertyTracker.set', 'dimension22', startTrialButton[1]);  // the referring 'start trial' button
        }

        ga('membershipPropertyTracker.set', 'dimension13', isCustomerAgent);  // customerAgent

        if(experience){
            ga('membershipPropertyTracker.set', 'dimension16', experience);   // Experience
        }

        if (camCodeBusinessUnit && camCodeBusinessUnit[1]) {
            ga('membershipPropertyTracker.set', 'dimension14', camCodeBusinessUnit[1]);  // CamCodeBusinessUnit
        }

        if (camCodeTeam && camCodeTeam[1]) {
            ga('membershipPropertyTracker.set', 'dimension15', camCodeTeam[1]);  // CamCodeTeam
        }

        ga('membershipPropertyTracker.send', 'pageview');

        flushEventQueue();
    }

    function flushEventQueue() {
        if (typeof(ga) === 'undefined') { return; }

        _EVENT_QUEUE.forEach(function (obj) {
            var upgrading = (obj.eventLabel === 'Rate Plan Change' && guardian.pageInfo.productData.initialProduct !== guardian.pageInfo.productData.productPurchasing) ? 1 : 0;
            var productName = guardian.pageInfo.productData.productPurchasing || guardian.pageInfo.productData.productPurchased;
            var event = {
                eventCategory: 'Subscriptions Checkout',
                eventAction:  guardian.pageInfo.productData.productType,
                eventLabel: obj.eventLabel,
                dimension11: guardian.pageInfo.productData.productType + ' - ' + productName,
                dimension13: !!guardian.supplierCode,
                metric1: upgrading,
                metric2: obj.elapsedTime,
            };
            if (guardian.pageInfo.productData.promoCode) {
                event.dimension19 = guardian.pageInfo.productData.promoCode;
            }
            if (guardian.experience){
                event.dimension16 = guardian.experience;
            }
            ga('membershipPropertyTracker.send', 'event', event);
        });

        _EVENT_QUEUE = [];
    }

    function enqueueEvent(eventLabel, elapsedTime) {
        _EVENT_QUEUE.push({ eventLabel: eventLabel, elapsedTime: elapsedTime });
        flushEventQueue();
    }

    return {
        init: analyticsEnabled(init),
        trackEvent: enqueueEvent,
        sourcePointVendorId: '5e542b3a4cd8884eb41b5a72'
    };
});
