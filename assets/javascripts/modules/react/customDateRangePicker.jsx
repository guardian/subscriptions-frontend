import React from 'react'
import DatePicker from 'react-datepicker'
import moment from 'moment'

export default class CustomDateRangePicker extends React.Component {

    displayName = 'CustomDateRangePicker';

    defaultProps = (() => {
        const maxWeeks = 6;
        return {
            remainingDays: (maxWeeks * 7),
            firstSelectableDate: moment(),
            lastStartDate: moment().add(1, 'year')
        }
    })();


    startDate = (() => {
        let startDate = moment(this.props.firstSelectableDate);
        while (this.props.filterDate && !this.props.filterDate(startDate)) {
            startDate.add(1, 'day')
        }
        return startDate
    })();

    calculateEndMaxDate = (startDate) => {
        var maxDate = moment(startDate); // i = 0 (1 remaining day)
        for (var i = 1; i < this.props.remainingDays; i++) {
            maxDate.add(1, 'day');
            if (this.props.filterDate && !this.props.filterDate(maxDate)) {
                i--
            }
        }
        return maxDate;
    };

    state = {
        startMinDate: this.startDate,
        startDate: this.startDate,
        endDate: this.startDate,
        endMaxDate: this.calculateEndMaxDate(this.startDate)
    };

    handleChange = ({startDate, endDate}) => {
        startDate = startDate || this.state.startDate;
        endDate = endDate || this.state.endDate;

        const endMaxDate = this.calculateEndMaxDate(startDate);

        if (startDate.isAfter(endDate)) {
            endDate = this.startDate;
        }

        this.setState({startDate, endDate, endMaxDate})
    };

    handleChangeStart = (startDate) => {
        this.handleChange({startDate})
    };

    handleChangeFinish = (endDate) => {
        this.handleChange({endDate})
    };

    render() {
        return <div>
            <div className="form-field">
                <label className="label" htmlFor="this.startDate">Starting on</label>
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
}
