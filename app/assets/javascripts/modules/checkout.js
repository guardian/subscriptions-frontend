define(['bean'], function (bean) {

    var IS_HIDDEN = 'is-hidden',
        HOUSE_ELEM = document.querySelector('.js-checkout-house'),
        STREET_ELEM = document.querySelector('.js-checkout-street'),
        TOWN_ELEM = document.querySelector('.js-checkout-town'),
        POSTCODE_ELEM = document.querySelector('.js-checkout-postcode'),
        FIND_ADDRESS_ELEM = document.querySelector('.js-checkout-find-address'),
        MANUAL_ADDRESS_ELEM = document.querySelector('.js-checkout-manual-address'),
        FULL_ADDRESS_ELEM = document.querySelector('.js-checkout-full-address');

    var findAddress = function () {
        bean.on(FIND_ADDRESS_ELEM, 'click', function (e) {
            e.preventDefault();
            e.stopImmediatePropagation();

            if (POSTCODE_ELEM.value) {
                // TODO: Ajax to lookup service
                populateAddressFields({
                    'house': 'Flat 14 Bankside House',
                    'street': 'West Hill',
                    'town': 'Putney'
                });
                showFullAddressFields();
            }
        });
    };

    var populateAddressFields = function (address) {
        HOUSE_ELEM.value = address.house;
        STREET_ELEM.value = address.street;
        TOWN_ELEM.value = address.town;
    };

    var manualAddress = function () {
        bean.on(MANUAL_ADDRESS_ELEM, 'click', function (e) {
            e.preventDefault();
            e.stopImmediatePropagation();

            showFullAddressFields();
            MANUAL_ADDRESS_ELEM.classList.add(IS_HIDDEN);
        });
    };

    var showFullAddressFields = function () {
        FULL_ADDRESS_ELEM.classList.remove(IS_HIDDEN);
    };

    function init() {
        findAddress();
        manualAddress();
    }

    return {
        init: init
    };

});
