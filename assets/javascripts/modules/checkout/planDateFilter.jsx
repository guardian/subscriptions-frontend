define([
    'modules/checkout/formElements',
], function (formElements) {
    'use strict';

    return function() {
        // Get the package name from the checked $PLAN_INPUTS
        var packageName = '';
        formElements.$PLAN_INPUTS.each((el) => (el.checked) && (packageName = el.getAttribute('data-option-mirror-package')));

        switch (true) {
            case /Sunday/i.test(packageName):
                return (date) => date.day() === 0;
            case /Sixday/i.test(packageName):
                return (date) => date.day() !== 0;
            case /Weekend/i.test(packageName):
                return (date) => date.day() === 6 || date.day() === 0;
            default:
                return (date) => true;
        }
    };
});
