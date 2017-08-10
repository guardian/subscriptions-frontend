/*global guardian*/
define([
    'modules/issue/formElements'
], function (
    formElements
) {
    'use strict';
    function init() {
        if (formElements.$ISSUE_REPORT_FORM.length) {
            require(['modules/issue/issueReportFields'], function(issueReportFields) {
                issueReportFields.default.init();
            });
        }
    }

    return {
        init: init
    };


});
