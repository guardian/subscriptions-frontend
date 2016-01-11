define([], function () {
    'use strict';

    var addressRules = function (option) {
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

    return {
        addressRules: addressRules
    };
});
