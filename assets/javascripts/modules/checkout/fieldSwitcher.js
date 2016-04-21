/*global guardian*/
define([
    '$',
    'bean',
    'modules/checkout/countryChoice',
    'modules/checkout/addressFields',
    'modules/checkout/localizationSwitcher',
    'modules/checkout/formElements'
], function ($, bean, countryChoice, addressFields, localizationSwitcher, formElements) {
    'use strict';

    var $postcode = $('.js-checkout-postcode'),
        $subdivision = $('.js-checkout-subdivision');

    var check = function(domEl) {
        $(domEl).attr('checked', 'checked');
        bean.fire(domEl, 'change');
    };

    var checkPlanInput = function (ratePlanId, currency) {
        formElements.$PLAN_INPUTS.each(function(input) {
            if ($(input).val() === ratePlanId && $(input).attr('data-currency') === currency) {
                check(input);
            }
        });
    };

    var redrawAddressFields = function(model) {
        var newPostcode = addressFields.postcode(
            model.postcodeRules.required,
            model.postcodeRules.label);

        var newSubdivision = addressFields.subdivision(
            model.subdivisionRules.required,
            model.subdivisionRules.label,
            model.subdivisionRules.values);

        newPostcode.input.value = model.postcode;

        $('input', $postcode).replaceWith(newPostcode.input);
        $('label', $postcode).replaceWith(newPostcode.label);

        $('#address-subdivision', $subdivision).replaceWith(newSubdivision.input);
        $('label', $subdivision).replaceWith(newSubdivision.label);
    };

    // Change the payment method to Direct Debit for UK users,
    // Credit Card for international users
    var selectPaymentMethod = function (country) {
        if (country === 'GB') {
            check(formElements.$DIRECT_DEBIT_TYPE[0]);
        } else {
            check(formElements.$CARD_TYPE[0]);
        }
    };

    var switchLocalization = function (model) {
        var currency = model.currency || guardian.currency;
        localizationSwitcher.set(currency, model.country);
    };

    var redraw = function(model) {
        redrawAddressFields(model);
        switchLocalization(model);
        checkPlanInput(model.ratePlanId, model.currency);
        selectPaymentMethod(model.country);
    };

    var getCurrentState = function() {
        var currentCountryOption = $(countryChoice.getCurrentCountryOption()),
            rules = countryChoice.addressRules();

        return {
            postcode: $('input', $postcode).val(),
            postcodeRules: rules.postcode,
            subdivisionRules: rules.subdivision,
            currency: currentCountryOption.attr('data-currency-choice'),
            country: currentCountryOption.val(),
            ratePlanId: formElements.getRatePlanId()
        };
    };

    var updateGuardianPageInfo = function(model) {
        var pageInfo = guardian.pageInfo;
        if (pageInfo) {
            pageInfo.billingCountry = model.country;
            pageInfo.billingCurrency = model.currency;
        }
    };

    var refresh = function () {
        var model = getCurrentState();
        redraw(model);
        updateGuardianPageInfo(model);
    };

    var refreshOnChange = function(el) {
        bean.on(el[0], 'change', function() {
            refresh();
        });
    };

    var init = function() {
        if (formElements.$CHECKOUT_FORM.length) {
            countryChoice.preselectCountry(guardian.country);
            refreshOnChange(formElements.$COUNTRY_SELECT);
            refresh();
        }
    };

    return {
        init: init
    };
});
