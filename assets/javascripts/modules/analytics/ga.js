/* global ga */
define(['utils/cookie'], function(cookie) {
    'use strict';

    return {
        init: function() {

            var identitySignedIn = !!cookie.getCookie('GU_U');
            var identitySignedOut = !!cookie.getCookie('GU_SO') && !identitySignedIn;

            /* Google analytics snippet */
            /*eslint-disable */
            (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
            (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
            m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
            })(window,document,'script','//www.google-analytics.com/analytics.js','ga');
            /*eslint-enable */

            ga('create', 'UA-51507017-5', 'auto');
            ga('set', 'dimension1', identitySignedIn.toString());
            ga('set', 'dimension2', identitySignedOut.toString());
            ga('send', 'pageview');
        }
    };
});
