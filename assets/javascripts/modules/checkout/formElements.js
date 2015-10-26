define(['$'], function ($) {
    'use strict';

    return {
        $CHECKOUT_FORM: $('.js-checkout-form'),

        $FIRST_NAME: $('.js-checkout-first .js-input'),
        $LAST_NAME: $('.js-checkout-last .js-input'),
        $EMAIL: $('.js-checkout-email .js-input'),
        $CONFIRM_EMAIL: $('.js-checkout-confirm-email .js-input'),
        $EMAIL_ERROR: $('.js-checkout-email .js-error-message'),
        $ADDRESS1: $('.js-checkout-house .js-input'),
        $ADDRESS2: $('.js-checkout-street .js-input'),
        $ADDRESS3: $('.js-checkout-town .js-input'),
        $POSTCODE: $('.js-checkout-postcode .js-input'),

        $PAYMENT_METHOD: $('.js-checkout-payment-method .js-option-switch'),

        // Direct Debit
        $ACCOUNT_CONTAINER: $('.js-checkout-account'),
        $SORTCODE_CONTAINER: $('.js-checkout-sortcode'),
        $HOLDER_CONTAINER: $('.js-checkout-holder'),
        $CONFIRM_PAYMENT_CONTAINER: $('.js-checkout-confirm-payment'),
        $ACCOUNT: $('.js-checkout-account .js-input'),
        $SORTCODE: $('.js-checkout-sortcode .js-input'),
        $HOLDER: $('.js-checkout-holder .js-input'),
        $CONFIRM_PAYMENT: $('.js-checkout-confirm-payment .js-input'),

        // Credit Card
        $CARD_NUMBER_CONTAINER: $('.js-checkout-card-number'),
        $CARD_CVC_CONTAINER: $('.js-checkout-card-cvc'),
        $CARD_EXPIRY_MONTH_CONTAINER: $('.js-checkout-card-expiry-month'),
        $CARD_EXPIRY_YEAR_CONTAINER: $('.js-checkout-card-expiry-year'),
        $CARD_NUMBER: $('.js-checkout-card-number .js-input'),
        $CARD_CVC: $('.js-checkout-card-cvc .js-input'),
        $CARD_EXPIRY_MONTH: $('.js-checkout-card-expiry-month .js-input'),
        $CARD_EXPIRY_YEAR: $('.js-checkout-card-expiry-year .js-input'),

        $FIRST_NAME_CONTAINER: $('.js-checkout-first'),
        $LAST_NAME_CONTAINER: $('.js-checkout-last'),
        $EMAIL_CONTAINER: $('.js-checkout-email'),
        $CONFIRM_EMAIL_CONTAINER: $('.js-checkout-confirm-email'),
        $ADDRESS1_CONTAINER: $('.js-checkout-house'),
        $ADDRESS2_CONTAINER: $('.js-checkout-street'),
        $ADDRESS3_CONTAINER: $('.js-checkout-town'),
        $POSTCODE_CONTAINER: $('.js-checkout-postcode'),
        $MANUAL_ADDRESS_CONTAINER: $('.js-checkout-manual-address'),
        $FULL_ADDRESS_CONTAINER: $('.js-checkout-full-address'),

        $YOUR_DETAILS_SUBMIT: $('.js-checkout-your-details-submit'),
        $PAYMENT_DETAILS_SUBMIT: $('.js-checkout-payment-details-submit'),
        $CHECKOUT_SUBMIT: $('.js-checkout-submit'),
        $REVIEW_NAME: $('.js-checkout-review-name'),
        $REVIEW_ADDRESS: $('.js-checkout-review-address'),
        $REVIEW_EMAIL: $('.js-checkout-review-email'),
        $REVIEW_ACCOUNT: $('.js-checkout-review-account'),
        $REVIEW_SORTCODE: $('.js-checkout-review-sortcode'),
        $REVIEW_HOLDER: $('.js-checkout-review-holder'),
        $FIELDSET_YOUR_DETAILS: $('.js-fieldset-your-details'),
        $FIELDSET_PAYMENT_DETAILS: $('.js-fieldset-payment-details'),
        $FIELDSET_REVIEW: $('.js-fieldset-review'),

        $EDIT_YOUR_DETAILS: $('.js-edit-your-details'),
        $EDIT_PAYMENT_DETAILS: $('.js-edit-payment-details')
    };
});
