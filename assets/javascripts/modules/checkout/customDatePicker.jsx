import React from 'react'
import DatePicker from 'react-datepicker'
import moment from 'moment'

import formElements from './formElements'

var NUMBER_OF_DAYS_IN_ADVANCE = 5;
var FIRST_SELECTABLE_DATE = moment().add(NUMBER_OF_DAYS_IN_ADVANCE, 'days');

var NUMBER_OF_DAYS_AVAILABLE = 21;
var LAST_SELECTABLE_DATE = moment().add(NUMBER_OF_DAYS_IN_ADVANCE, 'days').add(NUMBER_OF_DAYS_AVAILABLE, 'days');

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
                    locale="en"
                    selected={this.state.startDate}
                    onChange={this.handleChange}
                    minDate={FIRST_SELECTABLE_DATE}
                    maxDate={LAST_SELECTABLE_DATE}
                    {...this.props} />
    }
})
