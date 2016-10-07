define(['$'], function ($) {
    'use strict';

    var _PAPER_CHECKOUT_DATE_PICKER_FORM_FIELD_NAME = 'startDate';
    var _PAPER_CHECKOUT_DELIVERY_INSTRUCTIONS_FIELD_NAME = 'deliveryInstructions';

    var addressFields = function(relativeTo) {
        var $CONTAINER = $(relativeTo);
        var container = $CONTAINER[0];
        var $ADDRESS1_CONTAINER = $('.js-checkout-house', container);
        var $ADDRESS2_CONTAINER = $('.js-checkout-street', container);
        var $TOWN_CONTAINER = $('.js-checkout-town', container);
        var $COUNTRY_CONTAINER = $('.js-checkout-country', container);
        var $SUBDIVISION_CONTAINER = $('.js-checkout-subdivision', container);
        var $POSTCODE_CONTAINER = $('.js-checkout-postcode', container);
        var $ADDRESS_FINDER = $('.js-checkout-address-finder', container);
        var $ADDRESS_CHOOSER = $('.js-checkout-address-chooser', container);

        return {
            $CONTAINER: $CONTAINER,
            $ADDRESS1_CONTAINER: $ADDRESS1_CONTAINER,
            $ADDRESS2_CONTAINER: $ADDRESS2_CONTAINER,
            $TOWN_CONTAINER: $TOWN_CONTAINER,
            $COUNTRY_CONTAINER: $COUNTRY_CONTAINER,
            $SUBDIVISION_CONTAINER: $SUBDIVISION_CONTAINER,
            $POSTCODE_CONTAINER: $POSTCODE_CONTAINER,
            $ADDRESS_FINDER: $ADDRESS_FINDER,
            $ADDRESS_CHOOSER: $ADDRESS_CHOOSER,

            $ADDRESS1: $('.js-input', $ADDRESS1_CONTAINER[0]),
            $ADDRESS2: $('.js-input', $ADDRESS2_CONTAINER[0]),
            $TOWN: $('.js-input', $TOWN_CONTAINER[0]),
            $COUNTRY_SELECT: $('.js-country', $COUNTRY_CONTAINER[0]),
            getPostcode$: function() { return $('.js-input', $POSTCODE_CONTAINER[0]); },
            getSubdivision$: function() { return $('.js-input', $SUBDIVISION_CONTAINER[0]); }
        };
    };

    var getPaperCheckoutField = function() {
        return $('[name="' + _PAPER_CHECKOUT_DATE_PICKER_FORM_FIELD_NAME + '"]');
    };

    var getDeliveryInstructions = function() {
        return $('[name="' + _PAPER_CHECKOUT_DELIVERY_INSTRUCTIONS_FIELD_NAME + '"]');
    };

    return {
        $CHECKOUT_FORM: $('.js-checkout-form'),
        $NOTICES: $('.js-checkout-notices'),

        $SIGN_IN_LINK: $('.js-sign-in-link'),

        BILLING: addressFields('.js-billing-address'),
        DELIVERY: addressFields('.js-checkout-delivery'),

        $TITLE: $('.js-checkout-title .js-input'),
        $FIRST_NAME: $('.js-checkout-first .js-input'),
        $LAST_NAME: $('.js-checkout-last .js-input'),
        $EMAIL: $('.js-checkout-email .js-input'),
        $CONFIRM_EMAIL: $('.js-checkout-confirm-email .js-input'),
        $EMAIL_ERROR: $('.js-checkout-email .js-error-message'),
        $PHONE: $('.js-checkout-phone-number .js-input'),

        // Promo Code:
        $PROMO_CODE : $('.js-promo-code .js-input'),
        $PROMO_CODE_BTN: $('.js-promo-code-validate'),

        $DELIVERY_AS_BILLING: $('.js-checkout-use-delivery'),
        $PAYMENT_METHOD: $('.js-checkout-payment-method .js-option-switch'),

        // Direct Debit
        $ACCOUNT_CONTAINER: $('.js-checkout-account'),
        $DIRECT_DEBIT_TYPE: $('input[name="payment.type"][value="direct-debit"]'),
        $SORTCODE_CONTAINER: $('.js-checkout-sortcode'),
        $HOLDER_CONTAINER: $('.js-checkout-holder'),
        $CONFIRM_PAYMENT_CONTAINER: $('.js-checkout-confirm-payment'),
        $ACCOUNT: $('.js-checkout-account .js-input'),
        $SORTCODE: $('.js-checkout-sortcode .js-input'),
        $HOLDER: $('.js-checkout-holder .js-input'),
        $CONFIRM_PAYMENT: $('.js-checkout-confirm-payment .js-input'),

        // Credit Card
        $CARD_CONTAINER: $('.js-payment-type-card'),
        $CARD_TYPE: $('input[name="payment.type"][value="card"]'),
        $CARD_NUMBER_CONTAINER: $('.js-checkout-card-number'),
        $CARD_CVC_CONTAINER: $('.js-checkout-card-cvc'),
        $CARD_EXPIRY_CONTAINER: $('.js-checkout-card-expiry'),
        $CARD_NUMBER: $('.js-checkout-card-number .js-input'),
        $CARD_CVC: $('.js-checkout-card-cvc .js-input'),
        $CARD_EXPIRY_MONTH: $('.js-checkout-card-expiry-month .js-input'),
        $CARD_EXPIRY_YEAR: $('.js-checkout-card-expiry-year .js-input'),

        $FIRST_NAME_CONTAINER: $('.js-checkout-first'),
        $LAST_NAME_CONTAINER: $('.js-checkout-last'),
        $EMAIL_CONTAINER: $('.js-checkout-email'),
        $CONFIRM_EMAIL_CONTAINER: $('.js-checkout-confirm-email'),


        $BILLING_ADDRESS_SUBMIT: $('.js-checkout-billing-address-submit'),
        $YOUR_DETAILS_SUBMIT: $('.js-checkout-your-details-submit'),
        $PAYMENT_DETAILS_SUBMIT: $('.js-checkout-payment-details-submit'),
        $DELIVERY_DETAILS_SUBMIT: $('.js-checkout-delivery-details-submit'),
        $CHECKOUT_SUBMIT: $('.js-checkout-submit'),
        $FIELDSET_REVIEW: $('.js-fieldset-review'),
        $FIELDSET_YOUR_DETAILS: $('.js-fieldset-your-details'),
        $REVIEW_NAME: $('.js-checkout-review-name'),
        $REVIEW_ADDRESS: $('.js-checkout-review-address'),
        $REVIEW_EMAIL: $('.js-checkout-review-email'),
        $REVIEW_PHONE: $('.js-checkout-review-phone'),
        $REVIEW_PHONE_FIELD: $('.js-checkout-review-phone-field'),

        $FIELDSET_BILLING_ADDRESS: $('.js-fieldset-billing-address'),
        $FIELDSET_PAYMENT_DETAILS: $('.js-fieldset-payment-details'),
        $FIELDSET_DELIVERY_DETAILS: $('.js-fieldset-delivery-details'),
        $REVIEW_ACCOUNT: $('.js-checkout-review-account'),
        $REVIEW_SORTCODE: $('.js-checkout-review-sortcode'),
        $REVIEW_HOLDER: $('.js-checkout-review-holder'),
        $REVIEW_CARD_NUMBER: $('.js-checkout-review-card-number'),
        $REVIEW_CARD_EXPIRY: $('.js-checkout-review-card-expiry'),

        $REVIEW_DELIVERY_ADDRESS: $('.js-checkout-review-delivery-address'),
        $REVIEW_DELIVERY_INSTRUCTIONS: $('.js-checkout-review-delivery-instructions'),
        $REVIEW_DELIVERY_START_DATE: $('.js-checkout-review-delivery-start-date'),
        $EDIT_YOUR_DETAILS: $('.js-edit-your-details'),
        $EDIT_DELIVERY_DETAILS: $('.js-edit-your-delivery-details'),
        $EDIT_PAYMENT_DETAILS: $('.js-edit-payment-details'),
        $EDIT_BILLING_ADDRESS: $('.js-edit-billing-address'),

        // Paper checkout delivery details counter and date picker
        $DELIVERY_FIELDS: $('#deliveryFields'),
        $VOUCHER_FIELDS: $('#voucherFields'),
        DELIVERY_INSTRUCTIONS_ID: 'deliveryInstructionsCharacterCountedTextArea',
        PAPER_CHECKOUT_DATE_PICKER_ID: 'deliveryDatePicker',
        VOUCHER_CHECKOUT_DATE_PICKER_ID: 'deliveryDatePicker',
        $DELIVERED_PRODUCT_TYPE: $('input[name="delivered-product"]'),

        $BASKET: $('.js-basket'),

        getPaperCheckoutField: getPaperCheckoutField,
        getDeliveryInstructions: getDeliveryInstructions
    };
});
