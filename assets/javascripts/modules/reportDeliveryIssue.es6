/*global guardian*/
import formElements from 'modules/report/formElements'

export function init() {
    if (formElements.$REPORT_DELIVERY_ISSUE_FORM.length) {
        require(['modules/report/reportDeliveryIssueFields'], function (reportDeliveryIssueFields) {
            reportDeliveryIssueFields.init();
        })
    }
}

