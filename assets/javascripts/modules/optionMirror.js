define(['$'], function ($) {
    'use strict';

    var selectors = {
        OPTION_GROUP: '.js-option-mirror-group',
        MIRROR_VALUE: '.js-option-mirror-value'
    };

    function mirror(valueElems, input) {
        var selectedValue = input.getAttribute('data-option-mirror-label');
        if (selectedValue) {
            valueElems.each(function (el) {
                el.textContent = selectedValue;
            });
        }
    }

    var init = function() {
        var valueElems = $(selectors.MIRROR_VALUE);
        var options = $(selectors.OPTION_GROUP + ' input');
        if (valueElems && options.length) {
            options.each(function(option) {
                option.addEventListener('change', function(e) {
                    mirror(valueElems, e.target);
                });
                if (option.checked) {
                    mirror(valueElems, option);
                }
            });
        }
    };

    return {
        init: init
    };
});
