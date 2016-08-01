/* global ga */
define(['utils/cookie',
        'modules/analytics/analyticsEnabled'
    ], function(cookie, analyticsEnabled) {
    'use strict';
    function init() {

        var identitySignedIn = !!cookie.getCookie('GU_U');
        var identitySignedOut = !!cookie.getCookie('GU_SO') && !identitySignedIn;

        /* Google analytics snippet */
        /*eslint-disable */
        (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
            m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
        })(window,document,'script','//www.google-analytics.com/analytics.js','ga');
        /*eslint-enable */

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

        ga('set', 'dimension1', identitySignedIn.toString());   // deprecated
        ga('set', 'dimension2', identitySignedOut.toString());  // deprecated
        ga('send', 'pageview');

        ga('create', 'UA-44575989-1', 'auto', 'jellyfishGA');
        ga('jellyfishGA.send', 'pageview');
    }


    return {
        init: analyticsEnabled(init)
    };
});
