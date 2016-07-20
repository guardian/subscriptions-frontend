import React from 'react';
import ReactDOM from 'react-dom';

import formElements from './formElements'
import CustomDatePicker from '../react/customDatePicker'
import CharacterCountedTextArea from './characterCountedTextArea'
import moment from 'moment'

require('react-datepicker/dist/react-datepicker.css');

const NUMBER_OF_DAYS_IN_ADVANCE = 5;
const MAX_WEEKS_AVAILABLE = 4;

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

function getFirstSelectableDate(filterFn) {
    var firstSelectableDate = moment().add(NUMBER_OF_DAYS_IN_ADVANCE, 'days');
    while (filterFn && !filterFn(firstSelectableDate)) {
        firstSelectableDate = firstSelectableDate.add(1, 'days');
    }
    return firstSelectableDate;
}

function getLastSelectableDate(firstSelectableDate) {
    var startDate = firstSelectableDate || moment();
    return startDate.clone().add(MAX_WEEKS_AVAILABLE, 'weeks');
}

export default {
    renderDeliveryInstructions () {
        ReactDOM.render(
            <CharacterCountedTextArea name="deliveryInstructions" maxLength="250" className="input-text js-input" rows="4"/>,
            document.getElementById(formElements.DELIVERY_INSTRUCTIONS_ID)
        )
    },
    renderDatePicker () {
        const container = document.getElementById(formElements.PAPER_CHECKOUT_DATE_PICKER_ID),
              filterDateFn = filterDate(container.getAttribute('ratePlanName')),
              firstSelectableDate = getFirstSelectableDate(filterDateFn),
              lastSelectableDate = getLastSelectableDate(firstSelectableDate);

        ReactDOM.render(
            <CustomDatePicker name="startDate" className="input-text" dateFormat="D MMMM YYYY" locale="en-gb"
                              minDate={firstSelectableDate} maxDate={lastSelectableDate}
                              filterDate={filterDateFn} />,
            container
        )

    },
    init: function() {
        this.renderDeliveryInstructions();
        this.renderDatePicker();
    }
}
