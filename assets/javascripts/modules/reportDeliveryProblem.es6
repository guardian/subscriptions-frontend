/*global guardian*/
import formElements from 'modules/report/formElements'

export function init() {
    if (formElements.$REPORT_DELIVERY_PROBLEM_FORM.length) {
        require(['modules/report/reportDeliveryProblemFields'], function (reportDeliveryProblemFields) {
            reportDeliveryProblemFields.init();
        })
    }
}

