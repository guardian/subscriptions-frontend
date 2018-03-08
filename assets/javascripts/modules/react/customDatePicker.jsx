import React from 'react'
import DatePicker from 'react-datepicker'
import moment from 'moment'

export default class CustomDatePick extends React.Component {
    constructor(props) {
        super(props)

        this.state = {
            startDate: props.minDate || moment()
        }
    }

    render() {
        return <DatePicker
            selected={this.state.startDate}
            onChange={this.handleChange}
            inline
            {...this.props} />
    }
    
    handleChange = (date) => {
        this.setState({
            startDate: date
        })
    }
}