define(['$'], function ($) {
    'use strict';

    return function(addressObject) {

        var countrySelect = addressObject.$COUNTRY_SELECT[0];

        var getCurrentCountryOption = function () {
            return countrySelect.options[countrySelect.selectedIndex];
        };

        var addressRules = function (option) {
            option = option || getCurrentCountryOption();
            var postcodeRequired = option.getAttribute('data-postcode-required');
            var postcodeLabel = option.getAttribute('data-postcode-label');
            var subdivisionLabel = option.getAttribute('data-subdivision-label');
            var subdivisionRequired = option.getAttribute('data-subdivision-required');
            var list = option.getAttribute('data-subdivision-list');
            var subdivisionValues = list ? list.split(',') : [];

            return {
                postcode: {required: postcodeRequired === 'true', label: postcodeLabel},
                subdivision: {required: subdivisionRequired === 'true', values: subdivisionValues, label: subdivisionLabel}
            };
        };

        var preselectCountry = function(country) {
            $('option', countrySelect).each(function (el) {
                if (el.value === country) {
                    el.selected = true;
                }
            });
        };

        return {
            addressRules: addressRules,
            getCurrentCountryOption: getCurrentCountryOption,
            preselectCountry: preselectCountry
        };
    };
});
