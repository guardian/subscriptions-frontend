import React from 'react'
import ReactDOM from 'react-dom'
import moment from 'moment'
import formElements from './formElements'
import CustomDatePicker from '../react/customDatePicker'
import 'react-datepicker/dist/react-datepicker.css'

export function init() {
    const datePickerContainer = document.getElementById(formElements.REPORT_DELIVERY_PROBLEM_DATE_PICKER_ID);
    if (datePickerContainer != null) {
        ReactDOM.render(
            <CustomDatePicker name="issueDate"
                className="input-text"
                dateFormat="D MMMM YYYY"
                locale="en-gb"
                minDate={moment().subtract(1, 'weeks')}
                maxDate={moment()}
            />,
            datePickerContainer
        );
    }
}
