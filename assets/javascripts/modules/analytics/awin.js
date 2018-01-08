//Implementation docs here: https://drive.google.com/drive/folders/19AOGPkPbbFjqPJTdCcCba3EwpLiSHBwW
define([
    'modules/analytics/analyticsEnabled',
    'modules/analytics/dntEnabled'
], function (analyticsEnabled, dntEnabled) {
    'use strict';

    function init() {
        var AWIN = {};
        AWIN.Tracking = {};
        AWIN.Tracking.Sale = {};

        /*** Set your transaction parameters ***/
        const productData = guardian.pageInfo.productData
        AWIN.Tracking.Sale.amount = productData.amount;
        AWIN.Tracking.Sale.orderRef = productData.subscriptionId;
        AWIN.Tracking.Sale.parts = productData.productSegment + ':' + productData.amount;
        AWIN.Tracking.Sale.voucher = productData.promoCode;
        AWIN.Tracking.Sale.currency = productData.currency;
        AWIN.Tracking.Sale.test = '0';
        AWIN.Tracking.Sale.channel = 'aw';
    }

    return {
        init: analyticsEnabled(dntEnabled(init))
    };
});
