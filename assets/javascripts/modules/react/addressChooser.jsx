import React from 'react'
import PropTypes from 'prop-types';

function chooseAddress(addressSection, addressStr) {
    if (!addressSection) { return; }
    var address = addressStr.split(', ');
    if (address.length === 7) {
        addressSection.$ADDRESS1.val((address[0] + ' ' + address[1]).trim());
        addressSection.$ADDRESS2.val((address[2] + ' ' + address[3] + ' ' + address[4]).trim());
        addressSection.$TOWN.val(address[5].trim());
        addressSection.getSubdivision$().val(address[6].trim());
    }
}

export default class ChooseAddress extends React.Component {
    constructor(props) {
        super(props)
        this.state = {
            value: '-1'
        }
        if(props.options){
            this.options = [
                {
                    value: '-1',
                    name: 'Please select an address'
                },
                ... props.options
            ]
        } else {
            this.options = [

                {
                    value: '-1',
                    name: 'No address found'
                }
            ]
        }
        
    }

    handleChange = (e) => {
        let target = e.target;
        this.setState({ value: target.value });
        chooseAddress(this.props.addressSection, target.value);
        this.forceUpdate();
    }

    render() {
        let createItem = (item, key) => {
            return <option key={key} value={item.value}>{item.name}</option>;
        };
        return (
            <select className="select select--wide" onChange={this.handleChange} value={this.state.value}>
                {this.options.map(createItem)}
            </select>
        );
    }
}

ChooseAddress.propTypes = {
    addressSection: PropTypes.object.isRequired,
    options: PropTypes.arrayOf(PropTypes.shape({
        name: PropTypes.string,
        value: PropTypes.string
    }))
}