define([
    '$',
    'bean',
    'modules/checkout/countryChoice',
    'modules/checkout/addressFields'
], function ($, bean, countryChoice, addressFields) {
    'use strict';

    var countrySelect = function() {
        return $('.js-country');
    };

    var subdivision = function() {
        return $('.js-checkout-subdivision');
    };

    var postcode = function() {
        return $('.js-checkout-postcode');
    };

    var redraw = function(model) {
        var newPostcode = addressFields.postcode(model.postcodeRules.required, model.postcodeRules.label);
        var newSubdivision = addressFields.subdivision(model.subdivisionRules.required, model.subdivisionRules.label, model.subdivisionRules.values);

        newPostcode.input.value = model.postcode;
        newSubdivision.input.value = model.subdivision;

        $('input', postcode()).replaceWith(newPostcode.input);
        $('label', postcode()).replaceWith(newPostcode.label);

        $('#address-subdivision', subdivision()).replaceWith(newSubdivision.input);
        $('label', subdivision()).replaceWith(newSubdivision.label);

    };

    bean.on(countrySelect()[0], 'change', function(e) {
        var rules = countryChoice.addressRules(e.currentTarget.options[e.currentTarget.selectedIndex]);

        var state = {
            postcode: $('input', postcode()).val(),
            subdivision: $('select', subdivision()).val(),
            postcodeRules: rules.postcode,
            subdivisionRules: rules.subdivision
        };

        redraw(state);
    });

    return {
        init: function() {
        }
    };
});
