import React from 'react';
import ReactDOM from 'react-dom';
import moment from 'moment'
import formElements from './formElements'
import CustomDatePicker from '../react/customDatePicker'
import CharacterCountedTextArea from './characterCountedTextArea'
import reviewDetails from './reviewDetails'

require('react-datepicker/dist/react-datepicker.css');

const MAX_WEEKS_AVAILABLE = 4;

// The cut off for getting vouchers in two weeks is Wednesday at 6 AM GMT
const CUTOFF_WEEKDAY = 3;
const CUTOFF_HOUR = 6;

const NORMAL_DELIVERY_DELAY = 2;
const EXTRA_DELIVERY_DELAY  = 3;

const DELIVERY_DAY = 1; // Monday


function filterDate() {
    return (date) => date.day() === DELIVERY_DAY;
}

function getFirstSelectableDate() {
    var now = moment.utc();
    var currentWeekday = now.weekday();
    var currentHour = now.hour();
    var mostRecentMonday = moment().startOf('isoWeek');
    var weeksToAdd = currentWeekday >= CUTOFF_WEEKDAY && currentHour >= CUTOFF_HOUR ? EXTRA_DELIVERY_DELAY : NORMAL_DELIVERY_DELAY;
    return mostRecentMonday.add(weeksToAdd, 'weeks')
}

function getLastSelectableDate(firstSelectableDate) {
    var startDate = firstSelectableDate || moment();
    return moment(startDate).add(MAX_WEEKS_AVAILABLE, 'weeks');
}

function getDefaultProps() {
    const filterDateFn = filterDate(),
        firstSelectableDate = getFirstSelectableDate(),
        lastSelectableDate = getLastSelectableDate(firstSelectableDate);

    return {
        filterDate: filterDateFn,
        minDate: firstSelectableDate,
        maxDate: lastSelectableDate
    };
}

export default {
    renderDatePicker (container, defaultState) {
        if (!(container && defaultState)) return;

        return ReactDOM.render(
            <CustomDatePicker name="startDate" className="input-text" dateFormat="dddd D MMMM YYYY" locale="en-gb"
                              minDate={defaultState.minDate} maxDate={defaultState.maxDate } filterDate={defaultState.filterDate} />,
            container
        );
    },
    init: function() {
        const datePickerContainer = document.getElementById(formElements.VOUCHER_CHECKOUT_DATE_PICKER_ID);
        this.renderDatePicker(datePickerContainer, getDefaultProps());
    }
}
