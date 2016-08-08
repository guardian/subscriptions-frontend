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

        // Do JellyFish first as they don't want our custom dimensions
        ga('create', 'UA-44575989-1', 'auto', 'jellyfishGA');
        ga('jellyfishGA.send', 'pageview');

        ga('create', guardian.googleAnalytics.trackingId, {
            'allowLinker': true,
            'cookieDomain': guardian.googleAnalytics.cookieDomain
        });

        ga('require', 'linker');
        ga('linker:autoLink', ['eventbrite.co.uk'] ); // for consistency with the rest of membership sites

        /**
         * Enable enhanced link attribution
         * https://support.google.com/analytics/answer/2558867?hl=en-GB
         */
        ga('require', 'linkid', 'linkid.js');

        ga('set', 'dimension1', identitySignedIn.toString());   // deprecated - now uses dimension7 via util/user
        ga('set', 'dimension2', identitySignedOut.toString());  // deprecated - is logically equivalent to: dimension6 != "" and dimension7 === "false"
        (guardian.ophan) && ga('set', 'dimension3', guardian.ophan.pageViewId); // ophanPageview Id
        (ophanBrowserId) && ga('set', 'dimension4', ophanBrowserId); // ophanBrowserId
        ga('set', 'dimension5', 'subscriptions');               // platform
        (identitySignedIn) && ga('set', 'dimension6', user.getUserFromCookie().id); // identityId
        ga('set', 'dimension7', identitySignedIn.toString());   // isLoggedOn
        ga('send', 'pageview');
    }


    return {
        init: analyticsEnabled(init)
    };
});
