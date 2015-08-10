define(['$', 'bean', 'utils/masker'], function ($, bean, masker) {
    'use strict';

    var MASK_DELIM = 'data-masker-delim';
    var MASK_LENGTH = 'data-masker-length';

    function init() {
        var maskedEls = $('[' + MASK_DELIM + ']');
        maskedEls.each(function(el) {
            var delim = el.getAttribute(MASK_DELIM);
            var len = el.getAttribute(MASK_LENGTH);
            bean.on(el, 'keyup blur', masker(delim, len));
        });
    }

    return {
        init: init
    };

});
