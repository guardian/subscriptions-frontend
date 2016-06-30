define(['$'], function ($) {
    'use strict';
    var _PLAN_SELECT = $('.js-payment-frequency');
    var _PLAN_INPUTS = $('input[type="radio"]', _PLAN_SELECT);

    var getRatePlanId = function () {
        // Bonzo has no filter function :(
        var ratePlanId = null;
        _PLAN_INPUTS.each(function (input) {
            if ($(input).attr('checked')) {
                ratePlanId = input.value;
            }
        });
        return ratePlanId;
    };

    var addressFields = function(relativeTo) {

        var container = $(relativeTo)[0];
        var $ADDRESS1_CONTAINER = $('.js-checkout-house', container);
        var $ADDRESS2_CONTAINER = $('.js-checkout-street', container);
        var $ADDRESS3_CONTAINER = $('.js-checkout-town', container);
        var $SUBDIVISION_CONTAINER = $('.js-checkout-subdivision', container);
        var $POSTCODE_CONTAINER = $('.js-checkout-postcode', container);
        var $MANUAL_ADDRESS_CONTAINER = $('.js-checkout-manual-address', container); //wat

        return {
            $ADDRESS1_CONTAINER: $ADDRESS1_CONTAINER,
            $ADDRESS2_CONTAINER: $ADDRESS2_CONTAINER,
            $ADDRESS3_CONTAINER: $ADDRESS3_CONTAINER,
            $SUBDIVISION_CONTAINER: $SUBDIVISION_CONTAINER,
            $POSTCODE_CONTAINER: $POSTCODE_CONTAINER,
            $MANUAL_ADDRESS_CONTAINER: $MANUAL_ADDRESS_CONTAINER, //wat

            $ADDRESS1: $('.js-input', $ADDRESS1_CONTAINER[0]),
            $ADDRESS2: $('.js-input', $ADDRESS2_CONTAINER[0]),
            $ADDRESS3: $('.js-input', $ADDRESS3_CONTAINER[0]),
            $POSTCODE: $('.js-input', $POSTCODE_CONTAINER[0]),
            $SUBDIVISION: $('select', $SUBDIVISION_CONTAINER[0]),
            $COUNTRY_SELECT: $('.js-country', container)
        };
    };

    return {
        $CHECKOUT_FORM: $('.js-checkout-form'),
        $NOTICES: $('.js-checkout-notices'),

        BILLING: addressFields('.js-checkout-personal-address'),
        DELIVERY: addressFields('.js-checkout-delivery'),

        $FIRST_NAME: $('.js-checkout-first .js-input'),
        $LAST_NAME: $('.js-checkout-last .js-input'),
        $EMAIL: $('.js-checkout-email .js-input'),
        $CONFIRM_EMAIL: $('.js-checkout-confirm-email .js-input'),
        $EMAIL_ERROR: $('.js-checkout-email .js-error-message'),

        // Promo Code:
        $PROMO_CODE : $('.js-promo-code .js-input'),
        $PROMO_CODE_BTN: $('.js-promo-code-validate'),

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

        $YOUR_DETAILS_SUBMIT: $('.js-checkout-your-details-submit'),
        $PAYMENT_DETAILS_SUBMIT: $('.js-checkout-payment-details-submit'),
        $CHECKOUT_SUBMIT: $('.js-checkout-submit'),
        $FIELDSET_REVIEW: $('.js-fieldset-review'),
        $FIELDSET_YOUR_DETAILS: $('.js-fieldset-your-details'),
        $REVIEW_NAME: $('.js-checkout-review-name'),
        $REVIEW_ADDRESS: $('.js-checkout-review-address'),
        $REVIEW_EMAIL: $('.js-checkout-review-email'),

        $FIELDSET_PAYMENT_DETAILS: $('.js-fieldset-payment-details'),
        $REVIEW_ACCOUNT: $('.js-checkout-review-account'),
        $REVIEW_SORTCODE: $('.js-checkout-review-sortcode'),
        $REVIEW_HOLDER: $('.js-checkout-review-holder'),
        $REVIEW_CARD_NUMBER: $('.js-checkout-review-card-number'),
        $REVIEW_CARD_EXPIRY: $('.js-checkout-review-card-expiry'),

        $EDIT_YOUR_DETAILS: $('.js-edit-your-details'),
        $EDIT_PAYMENT_DETAILS: $('.js-edit-payment-details'),

        $BASKET: $('.js-basket'),
        $PLAN_INPUTS :_PLAN_INPUTS,

        getRatePlanId: getRatePlanId
    };
});
