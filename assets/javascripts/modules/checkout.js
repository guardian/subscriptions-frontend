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
    'modules/checkout/billingDetails',
    'modules/checkout/deliveryDetails',
    'modules/checkout/deliveryAsBilling'
], function (
    optionMirror,
    formElements,
    personalDetails,
    paymentDetails,
    editFieldsets,
    reviewDetails,
    promoCode,
    billingAddress,
    deliveryDetails,
    deliveryAsBilling
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
            deliveryDetails.init();
            deliveryAsBilling.init();
            billingAddress.init();
            if (formElements.$PAPER_CHECKOUT_DATE_PICKER.length) {
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
