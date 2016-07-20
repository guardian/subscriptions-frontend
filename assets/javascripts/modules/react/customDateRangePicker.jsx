import React from 'react'
import DatePicker from 'react-datepicker'
import moment from 'moment'

export default React.createClass({
    displayName: 'CustomDateRangePicker',
    getInitialState: function () {
        return {
            startDate: this.props.startDate,
            endDate: this.props.endDate
        }
    },

    handleChange: function ({ startDate, endDate }) {
        startDate = startDate || this.state.startDate
        endDate = endDate || this.state.endDate

        if (startDate.isAfter(endDate)) {
            var temp = startDate
            startDate = endDate
            endDate = temp
        }

        this.setState({ startDate, endDate })
    },

    handleChangeStart: function (startDate) {
        this.handleChange({ startDate })
    },

    handleChangeEnd: function (endDate) {
        this.handleChange({ endDate })
    },

    render: function () {
        return <div>
            <DatePicker
                    name="startDate"
                    selected={this.state.startDate}
                    startDate={this.state.startDate}
                    endDate={this.state.endDate}
                    onChange={this.handleChangeStart} {...this.props} />
            <DatePicker
                    name="endDate"
                    selected={this.state.endDate}
                    startDate={this.state.startDate}
                    endDate={this.state.endDate}
                    onChange={this.handleChangeEnd} {...this.props} />
        </div>
    }
})
