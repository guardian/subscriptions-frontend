define(function () {
    'use strict';

    var ERROR_CLASS = 'form-field--error';

    return function(container, condition) {
        if (condition) {
            container.addClass(ERROR_CLASS);
        } else {
            container.removeClass(ERROR_CLASS);
        }
    };

});
