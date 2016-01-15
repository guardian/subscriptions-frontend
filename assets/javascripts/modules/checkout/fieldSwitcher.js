/*global guardian*/
define([
    '$',
    'bean',
    'modules/checkout/countryChoice',
    'modules/checkout/addressFields',
    'modules/checkout/currencySwitcher',
    'modules/checkout/formElements'
], function ($, bean, countryChoice, addressFields, currencySwitcher, formElements) {
    'use strict';

    var check = function(domEl) {
        $(domEl).attr('checked', 'checked');
        bean.fire(domEl, 'change');
    };

    var subdivision = function() {
        return $('.js-checkout-subdivision');
    };

    var postcode = function() {
        return $('.js-checkout-postcode');
    };

    var $PLAN_INPUTS = $('input[type="radio"]', formElements.$PLAN_SELECT);

    var checkPlanInput = function (ratePlanId) {
        $PLAN_INPUTS.each(function(input) {
            if ($(input).val() === ratePlanId && !$(input).attr('disabled')) {
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
        newSubdivision.input.value = model.subdivision;

        $('input', postcode()).replaceWith(newPostcode.input);
        $('label', postcode()).replaceWith(newPostcode.label);

        $('#address-subdivision', subdivision()).replaceWith(newSubdivision.input);
        $('label', subdivision()).replaceWith(newSubdivision.label);
    };

    // Change the payment method to Direct Debit for UK users,
    // Credit Card for international users
    var selectPaymentMethod = function (currency) {
        if (currency == 'GBP') {
            check(formElements.$DIRECT_DEBIT_TYPE[0]);
        } else {
            check(formElements.$CARD_TYPE[0]);
        }
    };

    var switchCurrency = function (currency) {
        currencySwitcher.setCurrency(currency || guardian.currency);
    };

    var redraw = function(model) {
        redrawAddressFields(model);
        switchCurrency(model.currency);
        checkPlanInput(model.ratePlanId);
        selectPaymentMethod(model.currency);
    };

    var getRatePlanId = function () {
        // Bonzo has no filter function :(
        var ratePlanId = null;
        $PLAN_INPUTS.each(function (input) {
            if ($(input).attr('checked')) {
                ratePlanId = input.value;
            }
        });
        return ratePlanId;
    };

    var getCurrentState = function() {
        var countrySelect = formElements.$COUNTRY_SELECT[0];
        var currentCountryOption = countrySelect.options[countrySelect.selectedIndex];
        var rules = countryChoice.addressRules(currentCountryOption);

        return {
            postcode: $('input', postcode()).val(),
            subdivision: $('select, input', subdivision()).val(),
            postcodeRules: rules.postcode,
            subdivisionRules: rules.subdivision,
            currency: $(currentCountryOption).attr('data-currency-choice'),
            ratePlanId: getRatePlanId()
        };
    };

    var preselectCountry = function() {
        $('option', formElements.$COUNTRY_SELECT).each(function (el) {
            if ($(el).val() === guardian.country) {
                el.selected = true;
            }
        });
    };

    var refresh = function () {
        redraw(getCurrentState());
    };

    var refreshOnChange = function(el) {
        bean.on(el[0], 'change', function() {
            refresh();
        });
    };

    var init = function() {
        if (formElements.$CHECKOUT_FORM.length) {
            preselectCountry();
            refreshOnChange(formElements.$COUNTRY_SELECT);
            refresh();
        }
    };

    return {
        init: init
    };
});
