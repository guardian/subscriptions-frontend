define(['$', 'bean'], function ($, bean) {
    'use strict';

    var OPTION_SELECTOR = '.js-option-switch';
    var TARGET_ATTR = 'data-option-switch';

    // Returns a bonzo wrapped set
    function getTargets(el) {
        return $(el.getAttribute(TARGET_ATTR));
    }

    function init() {
        var optionEls = $(OPTION_SELECTOR);

        optionEls.each(function(el) {
            bean.on(el, 'change', function (e) {
                if (e.target.checked) {
                    // Unhide my targets
                    getTargets(e.target).removeAttr('hidden');

                    // Get my unchecked buddies
                    $('[name="' + e.target.name + '"]:not(:checked)').each(function(el) {
                        // Hide their targets
                        getTargets(el).attr('hidden', true);
                    });
                } else {
                    // Hide my targets.
                    // This should only happen for checkboxes because with radio
                    // buttons the 'change' event only fires when checked.
                    getTargets(e.target).attr('hidden', true);
                }
            });
        });
    }

    return {
        init: init
    };

});
