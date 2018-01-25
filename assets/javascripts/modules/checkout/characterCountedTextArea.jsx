import React from 'react'
import PropTypes from 'prop-types';

export default class CharacterCountedTextArea extends React.Component  {
    constructor(props) {
        super(props)

        this.state = {
            value: props.value,
        }
    }

    charsLeft = () => (this.props.maxLength - this.state.value.length)


    handleChange = (event) => {
        this.setState({ value: event.target.value.substring(0,this.props.maxLength) });
    }

    render() {
        return (
            <div>
                <textarea {...this.props} onChange={this.handleChange} value={this.state.value} />
                <small>Characters Left: {this.state.chars_left}</small>
            </div>
        );
    }
}

CharacterCountedTextArea.defaultProps = {
    maxLength: 1000,
    value: ''
}

CharacterCountedTextArea.propTypes = {
    maxLength: PropTypes.number,
    value: PropTypes.string
}