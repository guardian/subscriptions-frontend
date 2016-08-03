/*global guardian*/
define([
    'modules/suspend/formElements'
], function (
    formElements
) {
    'use strict';
    function init() {
        if (formElements.$SUSPEND_FORM.length) {
            require(['modules/suspend/suspendFields'], function(suspendFields) {
                suspendFields.default.init();
            });
        }
    }

    return {
        init: init
    };


});
