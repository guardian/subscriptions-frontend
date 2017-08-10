import React from 'react';
import ReactDOM from 'react-dom';
import moment from 'moment'
import formElements from './formElements'
import CustomDatePicker from '../react/customDatePicker'

require('react-datepicker/dist/react-datepicker.css');

export default {

    renderDatePicker (container) {

        return ReactDOM.render(
            <CustomDatePicker name="issueDate" className="input-text" dateFormat="D MMMM YYYY" locale="en-gb"
                              minDate={moment().subtract(1, 'weeks')} maxDate={moment()}
            />,
            container
        );
    },

    init: function() {
        const datePickerContainer = document.getElementById(formElements.ISSUE_REPORT_DATE_PICKER_ID);
        this.renderDatePicker(datePickerContainer);
    }

}
