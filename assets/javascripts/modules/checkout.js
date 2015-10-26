/*global Stripe*/
define([
    'modules/checkout/formElements',
    'modules/checkout/personalDetails',
    'modules/checkout/paymentDetails',
    'modules/checkout/editFieldsets',
    'modules/checkout/reviewDetails'
], function (
    formElements,
    personalDetails,
    paymentDetails,
    editFieldsets,
    reviewDetails
) {
    'use strict';

    function init() {
        if (formElements.$CHECKOUT_FORM.length) {
            personalDetails.init();
            paymentDetails.init();
            editFieldsets.init();
            reviewDetails.init();
        }

        require('js!stripe').then(function() {
            // TODO: IMPORTANT get this value from config
            Stripe.setPublishableKey('pk_test_Qm3CGRdrV4WfGYCpm0sftR0f');
        });
    }

    return {
        init: init
    };

});
