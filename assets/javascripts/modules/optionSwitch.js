define(['$', 'bean',
    'lodash/collection/reduce',
    'lodash/object/forOwn'
], function ($, bean, reduce, forOwn) {
    'use strict';

    var OPTION_SELECTOR = '.js-option-switch';
    var TARGET_ATTR = 'data-option-switch';

    function getAllTargets(els) {
        return reduce(els, function(result, current) {
            var id = current.getAttribute(TARGET_ATTR);
            result[id] = document.getElementById(id);
            return result;
        }, {});
    }

    function hideAllTargets(targets) {
        forOwn(targets, function(item) {
            item.setAttribute('hidden', true);
        });
    }

    function displayActive(el, targets) {
        if (el.checked) {
            hideAllTargets(targets);
            var id = el.getAttribute(TARGET_ATTR);
            var target = targets[id] || false;
            if (target) {
                target.removeAttribute('hidden');
            }
        }
    }

    function init() {
        var optionEls = $(OPTION_SELECTOR);
        var targets;

        if(!optionEls.length) {
            return;
        }

        targets = getAllTargets(optionEls);
        hideAllTargets(targets);
        optionEls.each(function (el) {
            displayActive(el, targets);
            bean.on(el, 'click', function() {
                displayActive(el, targets);
            });
        });
    }

    return {
        init: init
    };

});
