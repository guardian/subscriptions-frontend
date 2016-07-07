define(['$'], function ($) {
    'use strict';

    var selectors = {
        OPTION_GROUP: '.js-option-mirror-group',
        $MIRROR_VALUES: $('.js-option-mirror-payment-display'),
        $MIRROR_TITLES: $('.js-option-mirror-package-display')
    };

    function mirror(input) {
        var selectedValue = input.getAttribute('data-option-mirror-payment'),
            selectedTitle = input.getAttribute('data-option-mirror-package');
        if (selectedValue && selectedTitle) {
            selectors.$MIRROR_VALUES.each(function (el) {
                el.textContent = selectedValue;
            });
            selectors.$MIRROR_TITLES.each(function (el) {
                el.textContent = selectedTitle;
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
