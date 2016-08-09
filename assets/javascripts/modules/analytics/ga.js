/* global ga */
define(['modules/analytics/analyticsEnabled',
        'utils/cookie',
        'utils/user'
    ], function(analyticsEnabled, cookie, user) {
    'use strict';
    function init() {

        var identitySignedIn = user.isLoggedIn();
        var identitySignedOut = !!cookie.getCookie('GU_SO') && !identitySignedIn;
        var ophanBrowserId = cookie.getCookie('bwid');

        /* Google analytics snippet */
        /*eslint-disable */
        (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
            m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
        })(window,document,'script','//www.google-analytics.com/analytics.js','ga');
        /*eslint-enable */

        // Ours
        ga('create', {
            'trackingId': guardian.googleAnalytics.trackingId,
            'name': 'subscriptionsGA',
            'allowLinker': true,
            'cookieDomain': guardian.googleAnalytics.cookieDomain
        });

        ga('subscriptionsGA.require', 'linkid');
        ga('subscriptionsGA.require', 'linker');
        ga('subscriptionsGA.linker:autoLink', ['eventbrite.co.uk']);

        ga('subscriptionsGA.set', 'dimension1', identitySignedIn.toString());   // deprecated - now uses dimension7 via util/user
        ga('subscriptionsGA.set', 'dimension2', identitySignedOut.toString());  // deprecated - is logically equivalent to: dimension6 != "" and dimension7 === "false"
        (guardian.ophan) && ga('subscriptionsGA.set', 'dimension3', guardian.ophan.pageViewId); // ophanPageview Id
        (ophanBrowserId) && ga('subscriptionsGA.set', 'dimension4', ophanBrowserId); // ophanBrowserId
        ga('subscriptionsGA.set', 'dimension5', 'subscriptions');               // platform
        (identitySignedIn) && ga('subscriptionsGA.set', 'dimension6', user.getUserFromCookie().id); // identityId
        ga('subscriptionsGA.set', 'dimension7', identitySignedIn.toString());   // isLoggedOn

        ga('subscriptionsGA.send', 'pageview');

        // JellyFish
        ga('create', {
            'trackingId': 'UA-44575989-1',
            'name': 'jellyfishGA',
            'cookieDomain': 'auto'
        });
        ga('jellyfishGA.send', 'pageview');
    }


    return {
        init: analyticsEnabled(init)
    };
});
