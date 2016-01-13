define([
    '$',
    'bean',
    'modules/checkout/countryChoice',
    'modules/checkout/addressFields',
    'modules/checkout/currencySwitcher',
    'modules/checkout/formElements'
], function ($, bean, countryChoice, addressFields, currencySwitcher, formElements) {
    'use strict';

    var subdivision = function() {
        return $('.js-checkout-subdivision');
    };

    var postcode = function() {
        return $('.js-checkout-postcode');
    };

    var $PLAN_INPUTS = $('input[type="radio"]', formElements.$PLAN_SELECT);

    var redraw = function(model) {
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

        currencySwitcher.setCurrency(model.currency || guardian.currency);
        $PLAN_INPUTS.each(function(input) {
            if ($(input).val() === model.ratePlanId && !$(input).attr('disabled')) {
                $(input).attr('checked', 'checked');
                bean.fire(input, 'change');
            }
        });
    };

    var getRatePlanId = function () {
        // Don't try to make it smarter: Bonzo has no filter function :(
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
            subdivision: $('select', subdivision()).val(),
            postcodeRules: rules.postcode,
            subdivisionRules: rules.subdivision,
            currency: $(currentCountryOption).attr('data-currency-choice'),
            ratePlanId: getRatePlanId()
        };
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
            $('option', formElements.$COUNTRY_SELECT).each(function (el) {
                if ($(el).val() === guardian.country) {
                    el.selected = true;
                }
            });

            refreshOnChange(formElements.$COUNTRY_SELECT);
            refresh();
        }

    };

    return {
        init: init
    };
});
