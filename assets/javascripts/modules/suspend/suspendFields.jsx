import React from 'react';
import ReactDOM from 'react-dom';

import formElements from './formElements'
import CustomDateRangePicker from '../react/customDateRangePicker'

import moment from 'moment'

require('react-datepicker/dist/react-datepicker.css');

const LEAD_TIME = moment().add(5, 'days');
const LAST_START_DATE = moment().add(1, 'year');
const MAX_WEEKS = 6;

function getFirstSelectableDate(firstPaymentDate) {
    if (firstPaymentDate && moment(LEAD_TIME).isBefore(firstPaymentDate)) {
        return moment(firstPaymentDate);
    } else {
        return LEAD_TIME;
    }
}

function filterDate(packageName, exclusionList) {
    const exclusions = (exclusionList || '').split(',');
    const isNotTaken = (date) => !exclusions.includes(date.format('D MMMM YYYY'));

    switch (true) {
        case /Sunday/i.test(packageName):
            return (date) => date.day() === 0 && isNotTaken(date);
        case /Sixday/i.test(packageName):
            return (date) => date.day() !== 0 && isNotTaken(date);
        case /Weekend/i.test(packageName):
            return (date) => (date.day() === 6 || date.day() === 0) && isNotTaken(date);
        default:
            return isNotTaken;
    }
}

export default {

    renderDatePicker () {
        const container = document.getElementById(formElements.SUSPEND_DATE_PICKER_ID);
        if (!container) { return; }

        var firstSelectableDate = getFirstSelectableDate(container.getAttribute('firstPaymentDate')),
            remainingDays = parseInt(container.getAttribute('remainingDays'), 10),
            filterDateFn = filterDate(container.getAttribute('ratePlanName'), container.getAttribute('excludeExistingDays'));

        ReactDOM.render(
            <CustomDateRangePicker className="input-text"
                                   maxWeeks={MAX_WEEKS}
                                   firstSelectableDate={firstSelectableDate}
                                   lastStartDate={LAST_START_DATE}
                                   dateFormat="D MMMM YYYY" locale="en-gb"
                                   filterDate={filterDateFn}
                                   remainingDays={remainingDays}
            />,
            container
        )
    },
    init: function() {
        this.renderDatePicker();
    }
}
