/*global Stripe*/
/*global guardian*/
define([
    'modules/optionMirror',
    'modules/checkout/formElements',
    'modules/checkout/personalDetails',
    'modules/checkout/paymentDetails',
    'modules/checkout/editFieldsets',
    'modules/checkout/reviewDetails'
], function (
    optionMirror,
    formElements,
    personalDetails,
    paymentDetails,
    editFieldsets,
    reviewDetails
) {
    'use strict';
    function init() {
        if (formElements.$CHECKOUT_FORM.length) {
            formElements.$BASKET.each(function (el) {
                optionMirror.init(el);
            });
            personalDetails.init();
            paymentDetails.init();
            editFieldsets.init();
            reviewDetails.init();
        }

        require('js!stripe').then(function() {
            Stripe.setPublishableKey(guardian.stripePublicKey);
        });
    }

    return {
        init: init
    };

});
