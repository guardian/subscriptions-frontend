import React from 'react';
import ReactDOM from 'react-dom';
import moment from 'moment'
import formElements from './formElements'
import ratePlanChoice from './ratePlanChoice'
import planDateFilter from './planDateFilter'
import CustomDatePicker from '../react/customDatePicker'
import reviewDetails from './reviewDetails'

require('react-datepicker/dist/react-datepicker.css');

const MAX_WEEKS_AVAILABLE = 4;

// The cut off for getting vouchers in two weeks is Wednesday (day #3 in ISO format) at 6 AM GMT
const CUTOFF_WEEKDAY = 3;
const CUTOFF_HOUR = 6;

const NORMAL_DELIVERY_DELAY = 3;
const EXTRA_DELIVERY_DELAY  = 4;


function getFirstSelectableDate(filterFn) {
    const now = moment.utc();
    const currentWeekday = now.isoWeekday();
    const currentHour = now.hour();
    const mostRecentMonday = moment().startOf('isoWeek');
    const weeksToAdd = currentWeekday >= CUTOFF_WEEKDAY && currentHour >= CUTOFF_HOUR ? EXTRA_DELIVERY_DELAY : NORMAL_DELIVERY_DELAY;
    const firstSelectableDate = mostRecentMonday.add(weeksToAdd, 'weeks');
    while (filterFn && !filterFn(firstSelectableDate)) {
        firstSelectableDate.add(1, 'day');
    }
    return firstSelectableDate;
}

function getLastSelectableDate(firstSelectableDate) {
    const startDate = firstSelectableDate || moment();
    return moment(startDate).add(MAX_WEEKS_AVAILABLE, 'weeks');
}

function getDefaultProps() {
    const filterDateFn = planDateFilter(),
        firstSelectableDate = getFirstSelectableDate(filterDateFn),
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
    registerRatePlanChangeEvent (datePickerContainer, datePickerComponent) {
        if (!(datePickerContainer && datePickerComponent)) return; // don't register handlers if no component to update

        const self = this;

        ratePlanChoice.registerOnChangeAction(() => {

            // Reset the startDate (i.e. date of first paper) to the new minDate
            // iff the current selection is not in the new filter
            const defaultState = getDefaultProps();
            if (!defaultState.filterDate(datePickerComponent.state.startDate)) {
                datePickerComponent.handleChange(defaultState.minDate);
                reviewDetails.repopulateDetails();
            }

            self.renderDatePicker(datePickerContainer, defaultState);
        });
    },
    init: function() {

        const datePickerContainer = document.getElementById(formElements.VOUCHER_CHECKOUT_DATE_PICKER_ID);
        const datePickerComponent = this.renderDatePicker(datePickerContainer, getDefaultProps());

        // if the chosen rate plan changes, we need to reset the state in the datePickerComponent
        this.registerRatePlanChangeEvent(datePickerContainer, datePickerComponent);
    }
}
