define(function () {
    'use strict';

    var selectors = {
        OPTION_GROUP: '.js-option-mirror-group',
        MIRROR_VALUE: '.js-option-mirror-value'
    };

    function mirror(valueElems) {
        var selected = document.querySelector(selectors.OPTION_GROUP + ' input:checked');
        var selectedValue = selected.getAttribute('data-option-mirror-label');
        if (selectedValue) {
            [].forEach.call(valueElems, function(valueElem) {
                valueElem.textContent = selectedValue;
            });
        }
    }

    var init = function() {
        var valueElems = document.querySelectorAll(selectors.MIRROR_VALUE);
        var options = document.querySelectorAll(selectors.OPTION_GROUP + ' input');
        if (valueElems && options.length) {
            [].forEach.call(options, function(option) {
                option.addEventListener('change', function() {
                    mirror(valueElems);
                });
            });
            mirror(valueElems);
        }
    };

    return {
        init: init
    };
});
