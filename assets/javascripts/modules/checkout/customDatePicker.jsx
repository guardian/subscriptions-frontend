import React from 'react'
import DatePicker from 'react-datepicker'
import moment from 'moment'

import formElements from './formElements'

var NUMBER_OF_DAYS_IN_ADVANCE = 5;
var FIRST_SELECTABLE_DATE = moment().add(NUMBER_OF_DAYS_IN_ADVANCE, 'days');

export default React.createClass({
    displayName: 'CustomDatePick',
    getInitialState () {
        return {
            startDate: FIRST_SELECTABLE_DATE
        }
    },

    handleChange (date) {
        this.setState({
            startDate: date
        })
    },

    render () {
        return <DatePicker
                    dateFormat="YYYY-MM-DD"
                    selected={this.state.startDate}
                    onChange={this.handleChange}
                    minDate={FIRST_SELECTABLE_DATE}
                    className="input-text"
                    name={formElements.PAPER_CHECKOUT_DATE_PICKER_FORM_FIELD_NAME} />
    }
})
