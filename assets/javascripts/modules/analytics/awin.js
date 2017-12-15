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
        AWIN.Tracking.Sale.amount = '{{order_subtotal}}';
        AWIN.Tracking.Sale.orderRef = '{{order_ref}}';
        AWIN.Tracking.Sale.parts = '{{commission_group}}:{{sale_amount}}';
        AWIN.Tracking.Sale.voucher = '{{voucher_code}}';
        AWIN.Tracking.Sale.currency = '{{currency_code}}';
        AWIN.Tracking.Sale.test = '0';
        AWIN.Tracking.Sale.channel = 'aw';
    }

    return {
        //TODO - use analyticsEnabled
        test: analyticsEnabled(dntEnabled(init)),
        init: init
    };
});
