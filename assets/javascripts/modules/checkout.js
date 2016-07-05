/*global Stripe*/
/*global guardian*/
define([
    'modules/optionMirror',
    'modules/checkout/formElements',
    'modules/checkout/personalDetails',
    'modules/checkout/paymentDetails',
    'modules/checkout/editFieldsets',
    'modules/checkout/reviewDetails',
    'modules/checkout/promoCode',
    'modules/checkout/paperDeliveryAddress'
], function (
    optionMirror,
    formElements,
    personalDetails,
    paymentDetails,
    editFieldsets,
    reviewDetails,
    promoCode,
    paperDeliveryAddress
) {
    'use strict';
    function init() {
        if (formElements.$CHECKOUT_FORM.length) {
            optionMirror.init();
            personalDetails.init();
            paymentDetails.init();
            editFieldsets.init();
            reviewDetails.init();
            promoCode.init();
            paperDeliveryAddress.init();
            if (formElements.$PAPER_CHECKOUT_DATE_PICKER != null) {
                require(['modules/checkout/datePicker'], function(datePicker) {
                    datePicker.default.init();
                });
            }
        }

        curl('js!stripe').then(function() {
            Stripe.setPublishableKey(guardian.stripePublicKey);
        });
    }

    return {
        init: init
    };

});
