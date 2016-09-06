define([
    'modules/checkout/formElements',
], function (formElements) {
    'use strict';

    return function() {
        // Get the package name from the checked $PLAN_INPUTS
        var packageName = '';
        formElements.$PLAN_INPUTS.each((el) => (el.checked) && (packageName = el.getAttribute('data-option-mirror-package')));

        let deliveredProduct = formElements.$DELIVERED_PRODUCT_TYPE.val();
        let isPaper = 'paper' === deliveredProduct;

        let validDays = {
            voucher: {
                sixday: (date) => date.day() === 1,
                sunday: (date) => date.day() === 6,
                weekend: (date) => date.day() === 6,
                default: (date) => date.day() === 1
            },
            paper: {
                sixday: (date) => date.day() !== 0,
                sunday: (date) => date.day() === 0,
                weekend: (date) => date.day() === 6 || date.day() === 0,
                default: (date) => true
            }
        };

        let filters = isPaper ? validDays.paper : validDays.voucher;

        switch (true) {
            case /Sunday/i.test(packageName):
                return filters.sunday;
            case /Sixday/i.test(packageName):
                return filters.sixday;
            case /Weekend/i.test(packageName):
                return filters.weekend;
            default:
                return filters.default;
        }
    };
});
