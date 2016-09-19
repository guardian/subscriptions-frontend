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
    'modules/checkout/deliveryAsBilling',
    'modules/checkout/ratePlanChoice',
    'modules/checkout/eventTracking',
    'bean'
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
    deliveryAsBilling,
    ratePlanChoice,
    eventTracking,
    bean
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
            ratePlanChoice.init();
            eventTracking.init();
            if (formElements.$DELIVERY_FIELDS.length) {
                require(['modules/checkout/deliveryFields'], function(deliveryFields) {
                    deliveryFields.default.init();
                });
            } else if (formElements.$VOUCHER_FIELDS.length) {
                require(['modules/checkout/voucherFields'], function(voucherFields) {
                    voucherFields.default.init();
                });
            }
            // Prevent form submit on enter
            bean.on(formElements.$CHECKOUT_FORM[0], 'keypress', function (event) {
                if (event.keyCode === 13) {
                    event.preventDefault();
                    event.stopPropagation();
                }
            });
        }

        curl('js!stripe').then(function() {
            Stripe.setPublishableKey(guardian.stripePublicKey);
        });
    }

    return {
        init: init
    };

});
