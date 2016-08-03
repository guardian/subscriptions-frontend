import React from 'react';
import ReactDOM from 'react-dom';

import formElements from './formElements'
import CustomDateRangePicker from '../react/customDateRangePicker'

import moment from 'moment-business-days'

require('react-datepicker/dist/react-datepicker.css');

const FIRST_SELECTABLE_DATE = moment().businessAdd(5);
const MAX_WEEKS = 3;

function filterDate(packageName) {
    switch (true) {
        case /Sunday/i.test(packageName):
            return (date) => date.day() === 0;
        case /Sixday/i.test(packageName):
            return (date) => date.day() !== 0;
        case /Weekend/i.test(packageName):
            return (date) => date.day() === 6 || date.day() === 0;
        default:
            return (date) => true;
    }
}

export default {

    renderDatePicker () {
        const container = document.getElementById(formElements.SUSPEND_DATE_PICKER_ID),
            remainingDays = parseInt(container.getAttribute('remainingDays'), 10),
            filterDateFn = filterDate(container.getAttribute('ratePlanName'));

        ReactDOM.render(
            <CustomDateRangePicker className="input-text"
                                   maxWeeks={MAX_WEEKS}
                                   firstSelectableDate={FIRST_SELECTABLE_DATE}
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
