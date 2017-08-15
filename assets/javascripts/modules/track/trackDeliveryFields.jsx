import React from 'react'
import ReactDOM from 'react-dom'
import moment from 'moment'
import formElements from './formElements'
import CustomDatePicker from '../react/customDatePicker'
import 'react-datepicker/dist/react-datepicker.css'

function renderDatePicker(container) {

    return ReactDOM.render(
        <CustomDatePicker name="issueDate"
                          className="input-text"
                          dateFormat="D MMMM YYYY"
                          locale="en-gb"
                          minDate={moment().subtract(1, 'weeks')}
                          maxDate={moment()}
        />,
        container
    );
}

export function init() {
    const datePickerContainer = document.getElementById(formElements.TRACK_DELIVERY_DATE_PICKER_ID);
    renderDatePicker(datePickerContainer);
}

