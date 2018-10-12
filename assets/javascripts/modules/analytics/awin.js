//Implementation docs here: https://drive.google.com/drive/folders/19AOGPkPbbFjqPJTdCcCba3EwpLiSHBwW
define([
    'utils/cookie'
], function (cookie) {
    'use strict';

    function storeChannel() {
        const cookieExpiryDays = 30;
        const parsedUrl = new URL(window.location.href);
        const utmSource = parsedUrl.searchParams.get('utm_source');
        const utmMedium = parsedUrl.searchParams.get('utm_medium');
        const gclid = parsedUrl.searchParams.get('gclid'); // Google AdWords
        if (utmSource && utmMedium){
            cookie.setCookie('gu_referrer_channel', `${utmSource}&${utmMedium}`, cookieExpiryDays, false, true);
        } else if (gclid) {
            cookie.setCookie('gu_referrer_channel', 'google&adwords', cookieExpiryDays, false, true);
        }

    }

    return {
        init: storeChannel
    };
});
