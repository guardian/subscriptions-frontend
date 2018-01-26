import React from 'react'

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
            value: '?',
            options: [{
                value: '-1',
                name: 'No address found'
            }]
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
                {this.state.options.map(createItem)}
            </select>
        );
    }
}