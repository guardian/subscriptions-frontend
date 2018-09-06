//Implementation docs here: https://drive.google.com/drive/folders/19AOGPkPbbFjqPJTdCcCba3EwpLiSHBwW
define([
    'utils/cookie'
], function (cookie) {
    'use strict';

    function storeChannel() {
        const parsedUrl = new URL(window.location.href);
        const utmSource = parsedUrl.searchParams.get('utm_source');
        const utmMedium = parsedUrl.searchParams.get('utm_medium');
        if (utmSource && utmMedium){
            cookie.setCookie('gu_referrer_channel', `${utmSource}&${utmMedium}`, 30);
        }
    }

    return {
        init: storeChannel
    };
});
