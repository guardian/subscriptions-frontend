import React from 'react'

export default React.createClass({
    displayName: 'CharacterCountedTextArea',

    getDefaultProps () {
        return {
            maxLength: 1000
        }
    },

    getInitialState () {
        return {
            chars_left: this.props.maxLength
        }
    },

    handleChange (event) {
        var input = event.target.value;
        this.setState({
            chars_left: this.props.maxLength - input.length
        });
    },

    render () {
        return (
            <div>
                <textarea {...this.props} onChange={this.handleChange}></textarea>
                <small>Characters Left: {this.state.chars_left}</small>
            </div>
    );
    }
})
