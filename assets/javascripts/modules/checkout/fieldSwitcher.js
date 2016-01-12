define([
    '$',
    'bean',
    'modules/checkout/countryChoice',
    'modules/checkout/addressFields',
    'modules/checkout/currencySwitcher'
], function ($, bean, countryChoice, addressFields, currencySwitcher) {
    'use strict';

    var COUNTRY_SELECT = $('.js-country');

    var subdivision = function() {
        return $('.js-checkout-subdivision');
    };

    var postcode = function() {
        return $('.js-checkout-postcode');
    };

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
    };

    var getCurrentState = function() {
        var select = COUNTRY_SELECT[0];
        var currentOption = select.options[select.selectedIndex];
        var rules = countryChoice.addressRules(currentOption);

        return {
            postcode: $('input', postcode()).val(),
            subdivision: $('select', subdivision()).val(),
            postcodeRules: rules.postcode,
            subdivisionRules: rules.subdivision,
            currency: $(currentOption).attr('data-currency-choice')
        };
    };

    var refresh = function () {
        redraw(getCurrentState());
    };

    var init = function() {
        if (COUNTRY_SELECT.length) {
            $('option', COUNTRY_SELECT).each(function (el) {
                if ($(el).val() === guardian.country) {
                    el.selected = true;
                }
            });

            refresh();

            bean.on(COUNTRY_SELECT[0], 'change', function() {
                refresh();
            });
        }

    };

    return {
        init: init
    };
});
