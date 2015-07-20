define([
    'modules/checkout/formElements',
    'modules/checkout/toggleFieldsets',
    'modules/checkout/reviewDetails'
], function (
    formElements,
    toggleFieldsets,
    reviewDetails
) {
    'use strict';

    function init() {
        if(formElements.$CHECKOUT_FORM.length) {
            toggleFieldsets.init();
            reviewDetails.init();
        }
    }

    return {
        init: init
    };

});
