define([
    'modules/checkout/formElements',
    'modules/checkout/ratePlanChoice',
], function (formElements, ratePlanChoice) {
    'use strict';

    return function() {
        let packageName = ratePlanChoice.getSelectedRatePlanName();
        let deliveredProduct = formElements.$DELIVERED_PRODUCT_TYPE.val();

        let validDays = {
            voucher: {
                sixday: (date) => date.day() === 1,
                saturday: (date) => date.day() === 6,
                sunday: (date) => date.day() === 0,
                weekend: (date) => date.day() === 6,
                allDays: (date) => date.day() === 1
            },
            paper: {
                sixday: (date) => date.day() !== 0,
                saturday: (date) => date.day() === 6,
                sunday: (date) => date.day() === 0,
                weekend: (date) => date.day() === 6 || date.day() === 0,
                allDays: () => true
            },
            weekly: (date) => date.day() === 5
        };

        let filters = validDays[deliveredProduct];

        switch (true) {
            case /Weekly/i.test(packageName):
                return filters;
            case /Saturday/i.test(packageName):
                return filters.saturday;
            case /Sunday/i.test(packageName):
                return filters.sunday;
            case /Sixday/i.test(packageName):
                return filters.sixday;
            case /Weekend/i.test(packageName):
                return filters.weekend;
            default:
                return filters.allDays;
        }
    };
});
