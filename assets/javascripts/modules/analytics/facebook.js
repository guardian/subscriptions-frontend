/* global fbq */
define([
    'modules/analytics/analyticsEnabled',
    'modules/analytics/dntEnabled'
], function (analyticsEnabled, dntEnabled) {
    'use strict';

    function init() {

        /* Facebook pixel snippet */
        /*eslint-disable */
        (!function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){n.callMethod?
            n.callMethod.apply(n,arguments):n.queue.push(arguments)};if(!f._fbq)f._fbq=n;
            n.push=n;n.loaded=!0;n.version='2.0';n.queue=[];t=b.createElement(e);t.async=!0;
            t.src=v;s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}(window,
            document,'script','//connect.facebook.net/en_US/fbevents.js'));
        /*eslint-enable */

        fbq('init', '593826000802199');
        fbq('track', 'PageView');

        var productData = guardian.pageInfo ? guardian.pageInfo.productData : {};

        if (productData) {
            var parameters = {
                currency: 'GBP',
                value: '0',
                content_type: productData.productType
            };
            if (productData.productPurchasing) {
                parameters.content_name = productData.productPurchasing;
                fbq('track', 'InitiateCheckout', parameters);
            }
            if (productData.productPurchased) {
                parameters.content_name = productData.productPurchased;
                fbq('track', 'Purchase', parameters);
            }
        }
    }

    return {
        init: analyticsEnabled(dntEnabled(init))
    };
});
