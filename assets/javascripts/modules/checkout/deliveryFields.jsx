import React from 'react';
import ReactDOM from 'react-dom';
import moment from 'moment'
import formElements from './formElements'
import ratePlanChoice from './ratePlanChoice'
import planDateFilter from './planDateFilter'
import CustomDatePicker from '../react/customDatePicker'
import CharacterCountedTextArea from './characterCountedTextArea'
import reviewDetails from './reviewDetails'

require('react-datepicker/dist/react-datepicker.css');

const MAX_WEEKS_AVAILABLE = 3;

function getFirstSelectableDate(filterFn) {
    const weekly = formElements.$DELIVERED_PRODUCT_TYPE.val() === 'weekly';
    const NUMBER_OF_DAYS_IN_ADVANCE = weekly ? 9 : 3;
    const firstSelectableDate = moment().add(NUMBER_OF_DAYS_IN_ADVANCE, 'days');
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
    const filterDateFn = planDateFilter();
    const firstSelectableDate = getFirstSelectableDate(filterDateFn);
    const lastSelectableDate = getLastSelectableDate(firstSelectableDate);

    return {
        filterDate: filterDateFn,
        minDate: firstSelectableDate,
        maxDate: lastSelectableDate
    };
}

export default {
    renderDeliveryInstructions() {
        let deliveryInstructionElement = document.getElementById(formElements.DELIVERY_INSTRUCTIONS_ID);
        if (deliveryInstructionElement) {
            ReactDOM.render(
                <CharacterCountedTextArea name="deliveryInstructions" maxLength={250} className="input-text js-input" rows="4" />,
                deliveryInstructionElement
            )
        }
    },
    renderDatePicker(container, defaultState, curriedCallback) {
        if (!(container && defaultState)) { return; }

        ReactDOM.render(
            <CustomDatePicker name="startDate" className="input-text" dateFormat="dddd D MMMM YYYY" locale="en-gb"
                minDate={defaultState.minDate} maxDate={defaultState.maxDate}
                filterDate={defaultState.filterDate} />,
            container,
            curriedCallback(container)
        );
    },
    init: function () {
        this.renderDeliveryInstructions();

        // if the chosen rate plan changes, we need to reset the state in the datePickerComponent        
        const registerRatePlanChangeEvent = datePickerContainer => datePickerComponent => {
            if (!(datePickerContainer && datePickerComponent)) { return; } // don't register handlers if no component to update

            const self = this;

            ratePlanChoice.registerOnChangeAction(() => {

                // Reset the startDate (i.e. date of first paper) to the new minDate
                // iff the current selection is not in the new filter
                const defaultState = getDefaultProps();
                if (!defaultState.filterDate(datePickerComponent.state.startDate)) {
                    datePickerComponent.handleChange(defaultState.minDate);
                    reviewDetails.repopulateDetails();
                }

                self.renderDatePicker(datePickerContainer, defaultState, registerRatePlanChangeEvent);
            });
        };
        const datePickerContainer = document.getElementById(formElements.PAPER_CHECKOUT_DATE_PICKER_ID);
        this.renderDatePicker(datePickerContainer, getDefaultProps(), registerRatePlanChangeEvent);

    }
}
