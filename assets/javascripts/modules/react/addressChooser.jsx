import React from 'react'
import PropTypes from 'prop-types';

function chooseAddress(addressSection, addressStr) {
    if (!addressSection) {
        return;
    }
    var address = addressStr.split(', ');
    if (address.length === 7) {
        addressSection.$ADDRESS1.val((address[0] + ' ' + address[1]).trim());
        addressSection.$ADDRESS2.val((address[2] + ' ' + address[3] + ' ' + address[4]).trim());
        addressSection.$TOWN.val(address[5].trim());
        addressSection.getSubdivision$().val(address[6].trim());
    }
}

const augmentAddressOptions = (options) =>
    (options && options.length > 0) ? [
        {
            value: '-1',
            name: 'Please select an address'
        },
        ...options
    ] : [
        {
            value: '-1',
            name: 'No address found'
        }
    ];


const createAddressOption = (item, key) => <option key={key} value={item.value}>{item.name}</option>;

const selectAddressOption = (addressSection) => (event) =>  chooseAddress(addressSection, event.target.value);

const ChooseAddress = (props) =>
    <select className="select select--wide" onChange={selectAddressOption(props.addressSection)} value="-1">
        {augmentAddressOptions(props.options).map(createAddressOption)}
    </select>;

export default ChooseAddress;


ChooseAddress.propTypes = {
    addressSection: PropTypes.object.isRequired,
    options: PropTypes.arrayOf(PropTypes.shape({
        name: PropTypes.string,
        value: PropTypes.string
    }))
};
