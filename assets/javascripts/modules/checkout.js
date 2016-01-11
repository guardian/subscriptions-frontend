/*global Stripe*/
define([
    'modules/checkout/formElements',
    'modules/checkout/personalDetails',
    'modules/checkout/paymentDetails',
    'modules/checkout/editFieldsets',
    'modules/checkout/reviewDetails',
    'modules/checkout/fieldSwitcher'
], function (
    formElements,
    personalDetails,
    paymentDetails,
    editFieldsets,
    reviewDetails,
    fieldSwitcher
) {
    'use strict';

    function init() {
        if (formElements.$CHECKOUT_FORM.length) {
            personalDetails.init();
            paymentDetails.init();
            editFieldsets.init();
            reviewDetails.init();
            fieldSwitcher.init();
        }

        require('js!stripe').then(function() {
            Stripe.setPublishableKey(guardian.stripePublicKey);
        });
    }

    return {
        init: init
    };

});
