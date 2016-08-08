import React from 'react'
import DatePicker from 'react-datepicker'
import moment from 'moment'

export default React.createClass({
    displayName: 'CustomDateRangePicker',
    calculateEndMaxDate: function(startDate) {
        var maxDate = moment(startDate); // i = 0 (1 remaining day)
        for (var i = 1; i < this.props.remainingDays; i++) {
            maxDate.add(1, 'day');
            if (this.props.filterDate && !this.props.filterDate(maxDate)) {
                i--
            }
        }
        return maxDate;
    },
    getInitialState: function () {
        var startDate = moment(this.props.firstSelectableDate);
        while (this.props.filterDate && !this.props.filterDate(startDate)) {
            startDate.add(1, 'day')
        }
        return {
            startMinDate: startDate,
            startDate: startDate,
            endDate: startDate,
            endMaxDate: this.calculateEndMaxDate(startDate)
        }
    },
    getDefaultProps () {
        const maxWeeks = 6;
        return {
            remainingDays: (maxWeeks * 7),
            firstSelectableDate: moment(),
            lastStartDate: moment().add(1, 'year')
        }
    },
    handleChange: function ({ startDate, endDate }) {
        startDate = startDate || this.state.startDate;
        endDate = endDate || this.state.endDate;

        const endMaxDate = this.calculateEndMaxDate(startDate);

        if (startDate.isAfter(endDate)) {
            endDate = startDate;
        }

        this.setState({ startDate, endDate, endMaxDate })
    },

    handleChangeStart: function (startDate) {
        this.handleChange({ startDate })
    },

    handleChangeFinish: function (endDate) {
        this.handleChange({ endDate })
    },

    render: function () {
        return <div>
            <div className="form-field">
                <label className="label" htmlFor="startDate">Starting on</label>
                <DatePicker
                    name="startDate"
                    minDate={this.state.startMinDate}
                    maxDate={this.props.lastStartDate}
                    selected={this.state.startDate}
                    onChange={this.handleChangeStart} {...this.props} />
            </div>
            <div className="form-field">
                <label className="label" htmlFor="endDate">Finishing on</label>
                <DatePicker
                    name="endDate"
                    minDate={this.state.startDate}
                    maxDate={this.state.endMaxDate}
                    selected={this.state.endDate}
                    onChange={this.handleChangeFinish} {...this.props} />
            </div>
        </div>
    }
})
