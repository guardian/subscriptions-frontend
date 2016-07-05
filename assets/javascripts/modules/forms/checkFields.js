define(['modules/forms/toggleError', '$'], function (toggleError, $) {
    'use strict';

    return {
        checkRequiredFields: function (context) {
            return $('input[required]:not([disabled]), select[required]:not([disabled])', context[0]).map(function (f) {
                return $(f);
            }).map(function ($field) {
                toggleError($field.parent(), !$field.val());
                return $field.val();
            }).reduce(function (f1, f2) {
                return f1 && f2;
            }, true);
        }
    };
});
