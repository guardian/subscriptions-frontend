define(['$'], function ($) {
    'use strict';

    var selectors = {
        OPTION_GROUP: '.js-option-mirror-group',
        $MIRROR_PACKAGE: $('.js-option-mirror-package-display'),
        $MIRROR_DESCRIPTION: $('.js-option-mirror-description-display'),
        $MIRROR_PAYMENT: $('.js-option-mirror-payment-display')
    };

    function mirror(input) {
        var selectedPackage = input.getAttribute('data-option-mirror-package'),
            selectedDescription = input.getAttribute('data-option-mirror-description'),
            selectedPayment = input.getAttribute('data-option-mirror-payment');

        if (selectedPackage && selectedPayment) {
            selectors.$MIRROR_PACKAGE.each(function (el) {
                el.textContent = selectedPackage;
            });
            selectors.$MIRROR_DESCRIPTION.each(function (el) {
                el.textContent = selectedDescription;
            });
            selectors.$MIRROR_PAYMENT.each(function (el) {
                el.textContent = selectedPayment;
            });
        }
    }

    var init = function() {
        var options = $(selectors.OPTION_GROUP + ' input');
        if (options.length) {
            options.each(function(option) {
                option.addEventListener('change', function(e) {
                    mirror(e.target);
                });
                if (option.checked) {
                    mirror(option);
                }
            });
        }
    };

    return {
        init: init
    };
});
