import React from 'react'
import DatePicker from 'react-datepicker'
import moment from 'moment'

export default React.createClass({
    displayName: 'CustomDatePick',

    getDefaultProps () {
        return {
            minDate: moment()
        }
    },

    getInitialState () {
        return {
            startDate: this.props.minDate
        }
    },

    handleChange (date) {
        this.setState({
            startDate: date
        })
    },

    render () {
        return <DatePicker
                    selected={this.state.startDate}
                    onChange={this.handleChange}
                    {...this.props} />
    }
})
