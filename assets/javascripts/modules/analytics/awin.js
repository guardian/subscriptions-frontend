//Implementation docs here: https://drive.google.com/drive/folders/19AOGPkPbbFjqPJTdCcCba3EwpLiSHBwW
define([
    'modules/analytics/analyticsEnabled',
    'modules/analytics/dntEnabled',
    'utils/cookie'
], function (analyticsEnabled, dntEnabled, cookie) {
    'use strict';

    const referrerCookieName = 'gu_referrer_channel';

    function init() {
        storeChannel();

        if (guardian.pageInfo.pageType !== 'Thankyou') {
            return;
        }

        let AWIN = {};
        AWIN.Tracking = {};
        AWIN.Tracking.Sale = {};

        /*** Set your transaction parameters ***/
        const productData = guardian.pageInfo.productData;
        AWIN.Tracking.Sale.amount = productData.amount;
        AWIN.Tracking.Sale.orderRef = productData.subscriptionId;
        AWIN.Tracking.Sale.parts = productData.productSegment + ':' + productData.amount;
        AWIN.Tracking.Sale.voucher = productData.promoCode;
        AWIN.Tracking.Sale.currency = productData.currency;
        AWIN.Tracking.Sale.test = '0';
        AWIN.Tracking.Sale.channel = getAwinChannel();
        window.AWIN = AWIN;
    }

    function getAwinChannel() {
        const channel = cookie.getCookie(referrerCookieName);
        if (channel === null) {
            return 'direct'; //No referrer the user came direct to the site
        }

        const values = channel ? channel.split('&') : null;
        if (values && values[0] === 'afl' && values[1] === 'awin'){
            return 'aw';
        }
        return 'na'; //A referrer other than Awin
    }

    function storeChannel() {
        const parsedUrl = new URL(window.location.href);
        const utmSource = parsedUrl.searchParams.get('utm_source');
        const utmMedium = parsedUrl.searchParams.get('utm_medium');
        if (utmSource && utmMedium){
            cookie.setCookie(referrerCookieName, `${utmSource}&${utmMedium}`);
        }
    }

    return {
        init: analyticsEnabled(dntEnabled(init))
    };
});
