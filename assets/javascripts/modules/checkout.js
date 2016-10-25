/*global Stripe*/
/*global guardian*/
define([
    'modules/optionMirror',
    'modules/checkout/formElements',
    'modules/checkout/personalDetails',
    'modules/checkout/paymentDetails',
    'modules/checkout/editFieldsets',
    'modules/checkout/reviewDetails',
    'modules/checkout/submit',
    'modules/checkout/promoCode',
    'modules/checkout/billingDetails',
    'modules/checkout/deliveryDetails',
    'modules/checkout/fieldSwitcher',
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
    submit,
    promoCode,
    billingAddress,
    deliveryDetails,
    fieldSwitcher,
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
            fieldSwitcher.init();
            paymentDetails.init();
            editFieldsets.init();
            reviewDetails.init();
            submit.init();
            promoCode.init();
            deliveryDetails.init();
            deliveryAsBilling.init();
            billingAddress.init();
            ratePlanChoice.init();
            eventTracking.init();
            if (formElements.$DELIVERY_FIELDS.length) {
                require(['modules/checkout/deliveryFields', 'modules/checkout/addressFinder'], function(deliveryFields, addressFinder) {
                    deliveryFields.default.init();
                    addressFinder.default.init();
                });
            } else if (formElements.$VOUCHER_FIELDS.length) {
                require(['modules/checkout/voucherFields', 'modules/checkout/addressFinder'], function(voucherFields, addressFinder) {
                    voucherFields.default.init();
                    addressFinder.default.init();
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
        if(guardian.stripeCheckout){
            curl('js!stripeCheckout');
        } else {
            curl('js!stripe').then(function () {
                Stripe.setPublishableKey(guardian.stripePublicKey);
            });
        }
    }

    return {
        init: init
    };

});
