import React from 'react';
import ReactDOM from 'react-dom';

import formElements from './formElements'
import CustomDateRangePicker from '../react/customDateRangePicker'

import moment from 'moment'

require('react-datepicker/dist/react-datepicker.css');

const NUMBER_OF_DAYS_IN_ADVANCE = 15;
const MAX_WEEKS_AVAILABLE = 12;

const FIRST_SELECTABLE_DATE = moment().add(NUMBER_OF_DAYS_IN_ADVANCE, 'days')
const DEFAULT_END_DATE = moment().add(NUMBER_OF_DAYS_IN_ADVANCE + 2, 'days')
const LAST_SELECTABLE_DATE = moment().add(NUMBER_OF_DAYS_IN_ADVANCE + 7 * MAX_WEEKS_AVAILABLE, 'days')

export default {
    renderDatePicker () {
        ReactDOM.render(
            <CustomDateRangePicker className="input-text" startDate={FIRST_SELECTABLE_DATE} minDate={FIRST_SELECTABLE_DATE} endDate={DEFAULT_END_DATE} maxDate={LAST_SELECTABLE_DATE} dateFormat="D MMMM YYYY" locale="en-gb" />,
            document.getElementById(formElements.SUSPEND_DATE_PICKER_ID)
        )

    },
    init: function() {
        this.renderDatePicker();
    }
}
