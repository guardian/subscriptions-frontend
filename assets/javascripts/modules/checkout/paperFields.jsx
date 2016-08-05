import React from 'react';
import ReactDOM from 'react-dom';
import moment from 'moment'
import formElements from './formElements'
import CustomDatePicker from '../react/customDatePicker'
import CharacterCountedTextArea from './characterCountedTextArea'
import reviewDetails from './reviewDetails'

require('react-datepicker/dist/react-datepicker.css');

const NUMBER_OF_DAYS_IN_ADVANCE = 5;
const MAX_WEEKS_AVAILABLE = 4;

function filterDate() {
    // Get the package name from the checked $PLAN_INPUTS
    var packageName = '';
    formElements.$PLAN_INPUTS.each((el) => (el.checked) && (packageName = el.getAttribute('data-option-mirror-package')));

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
        firstSelectableDate.add(1, 'day');
    }
    return firstSelectableDate;
}

function getLastSelectableDate(firstSelectableDate) {
    var startDate = firstSelectableDate || moment();
    return moment(startDate).add(MAX_WEEKS_AVAILABLE, 'weeks');
}

function getDefaultProps() {
    const filterDateFn = filterDate(),
        firstSelectableDate = getFirstSelectableDate(filterDateFn),
        lastSelectableDate = getLastSelectableDate(firstSelectableDate);

    return {
        filterDate: filterDateFn,
        minDate: firstSelectableDate,
        maxDate: lastSelectableDate
    };
}

export default {
    renderDeliveryInstructions () {
        ReactDOM.render(
            <CharacterCountedTextArea name="deliveryInstructions" maxLength="250" className="input-text js-input" rows="4"/>,
            document.getElementById(formElements.DELIVERY_INSTRUCTIONS_ID)
        )
    },
    renderDatePicker (container, defaultState) {
        if (!(container && defaultState)) return;

        return ReactDOM.render(
            <CustomDatePicker name="startDate" className="input-text" dateFormat="dddd D MMMM YYYY" locale="en-gb"
                              minDate={defaultState.minDate} maxDate={defaultState.maxDate}
                              filterDate={defaultState.filterDate} />,
            container
        );
    },
    registerRatePlanChangeEvent (datePickerContainer, datePickerComponent) {
        if (!(datePickerContainer && datePickerComponent)) return; // don't register handlers if no component to update

        const self = this;

        formElements.$PLAN_INPUTS.each((el) => el.addEventListener('change', () => {

            // Reset the startDate (i.e. date of first paper) to the new minDate
            // iff the current selection is not in the new filter
            const defaultState = getDefaultProps();
            if (!defaultState.filterDate(datePickerComponent.state.startDate)) {
                datePickerComponent.handleChange(defaultState.minDate);
                reviewDetails.repopulateDetails();
            }

            self.renderDatePicker(datePickerContainer, defaultState);
        }));
    },
    init: function() {
        this.renderDeliveryInstructions();

        const datePickerContainer = document.getElementById(formElements.PAPER_CHECKOUT_DATE_PICKER_ID);
        const datePickerComponent = this.renderDatePicker(datePickerContainer, getDefaultProps());

        // if the chosen rate plan changes, we need to reset the state in the datePickerComponent
        this.registerRatePlanChangeEvent(datePickerContainer, datePickerComponent);
    }
}
