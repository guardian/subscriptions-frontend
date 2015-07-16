define(function () {
    'use strict';

    var selectors = {
        OPTION_GROUP: '.js-option-mirror-group',
        MIRROR_VALUE: '.js-option-mirror-value'
    };

    function mirror(valueElem) {
        var selected = document.querySelector(selectors.OPTION_GROUP + ' input:checked');
        var selectedValue = selected.getAttribute('data-option-mirror-label');
        if(selectedValue) {
            valueElem.textContent = selectedValue;
        }
    }

    var init = function() {
        var valueElem = document.querySelector(selectors.MIRROR_VALUE);
        var options = document.querySelectorAll(selectors.OPTION_GROUP + ' input');
        if (valueElem && options.length) {
            [].forEach.call(options, function(option) {
                option.addEventListener('change', function() {
                    mirror(valueElem);
                });
            });
            mirror(valueElem);
        }
    };

    return {
        init: init
    };
});
