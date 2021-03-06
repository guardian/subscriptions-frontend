import ajax from 'ajax';
import React from 'react';
import ReactDOM from 'react-dom';
import formEls from './formElements';
import ChooseAddress from '../react/addressChooser';

function showNothingFound(addressSection) {
    ReactDOM.render(
    <ChooseAddress addressSection={addressSection} />,
    addressSection.$ADDRESS_CHOOSER[0]);
}

function fillInChooseAddressBox(addressSection, addresses) {
    let options = addresses.map(address => ({name: address.replace(/[, ]+/g, ', '), value: address}))
    ReactDOM.render(<ChooseAddress addressSection={addressSection} options={options} />, addressSection.$ADDRESS_CHOOSER[0]);
}

function getSantitisedPostcode(addressSection) {
    var $POSTCODE = addressSection.getPostcode$();
    if ($POSTCODE.length !== 1) { return ''; }
    return $POSTCODE.val().replace(/\s/g, '').toUpperCase();
}

function lookup(addressSection) {
    return (e) => {
        e.stopPropagation();
        e.preventDefault();
        var postCode = getSantitisedPostcode(addressSection);
        if (postCode.length < 3) { return; }

        ajax({
            type: 'json',
            method: 'GET',
            url: '/checkout/findAddress',
            headers: {
                'Csrf-Token': document.querySelector('input[name="csrfToken"]').getAttribute('value')
            },
            data: {
                'postCode': postCode
            },
            timeout: 1500
        }).then((response) => {
            // Capital A 'Addresses' is for compatibility with https://api.getaddress.io/v2/uk/ should we want not to
            // proxy via our server.
            if (response && response.Addresses && response.Addresses.length > 0) {
                fillInChooseAddressBox(addressSection, response.Addresses);
            } else {
                showNothingFound(addressSection);
            }
        }).catch(() => showNothingFound(addressSection));
    };
}

export default {
    init () {
        var section = formEls['DELIVERY'],
            $ADDRESS_FINDER = section.$ADDRESS_FINDER;

        if ($ADDRESS_FINDER.length === 1) {
            $ADDRESS_FINDER[0].addEventListener('click', lookup(section));
        }
    }
}
