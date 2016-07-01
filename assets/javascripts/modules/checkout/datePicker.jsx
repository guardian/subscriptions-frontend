import React from 'react';
import ReactDOM from 'react-dom';
import DatePicker from 'react-datepicker';
import moment from 'moment';

require('react-datepicker/dist/react-datepicker.css');

var DATE_PICKER_ID = 'deliveryDatePicker';
var FORM_FIELD_NAME = 'startDate';
var NUMBER_OF_DAYS_IN_ADVANCE = 5;
var FIRST_SELECTABLE_DATE = moment().add(NUMBER_OF_DAYS_IN_ADVANCE, 'days');

export default {
        startDate: FIRST_SELECTABLE_DATE,
        handleChange: e => e,
        init: function() {
            ReactDOM.render(
                <DatePicker
                    dateFormat="YYYY-MM-DD"
                    selected={this.startDate}
                    onChange={this.handleChange}
                    minDate={FIRST_SELECTABLE_DATE}
                    className="input-text"
                    name={FORM_FIELD_NAME}
                />,
                document.getElementById(DATE_PICKER_ID)
            )
        }
}
