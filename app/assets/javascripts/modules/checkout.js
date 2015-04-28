define(['bean'], function (bean) {

    var IS_HIDDEN = 'is-hidden',
        FIRST_NAME_ELEM = document.querySelector('.js-checkout-first'),
        LAST_NAME_ELEM = document.querySelector('.js-checkout-last'),
        EMAIL_ELEM = document.querySelector('.js-checkout-email'),
        HOUSE_ELEM = document.querySelector('.js-checkout-house'),
        STREET_ELEM = document.querySelector('.js-checkout-street'),
        TOWN_ELEM = document.querySelector('.js-checkout-town'),
        POSTCODE_ELEM = document.querySelector('.js-checkout-postcode'),
        ACCOUNT_ELEM = document.querySelector('.js-checkout-account'),
        SORTCODE1_ELEM = document.querySelector('.js-checkout-sortcode1'),
        SORTCODE2_ELEM = document.querySelector('.js-checkout-sortcode2'),
        SORTCODE3_ELEM = document.querySelector('.js-checkout-sortcode3'),
        HOLDER_ELEM = document.querySelector('.js-checkout-holder'),
        FIND_ADDRESS_ELEM = document.querySelector('.js-checkout-find-address'),
        MANUAL_ADDRESS_ELEM = document.querySelector('.js-checkout-manual-address'),
        FULL_ADDRESS_ELEM = document.querySelector('.js-checkout-full-address'),
        YOUR_DETAILS_SUBMIT_ELEM = document.querySelector('.js-checkout-your-details-submit'),
        PAYMENT_DETAILS_SUBMIT_ELEM = document.querySelector('.js-checkout-payment-details-submit'),
        REVIEW_NAME_ELEM = document.querySelector('.js-checkout-review-name'),
        REVIEW_ADDRESS_ELEM = document.querySelector('.js-checkout-review-address'),
        REVIEW_EMAIL_ELEM = document.querySelector('.js-checkout-review-email'),
        REVIEW_ACCOUNT_ELEM = document.querySelector('.js-checkout-review-account'),
        REVIEW_SORTCODE_ELEM = document.querySelector('.js-checkout-review-sortcode'),
        REVIEW_HOLDER_ELEM = document.querySelector('.js-checkout-review-holder');

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

    var reviewDetails = function () {
        bean.on(YOUR_DETAILS_SUBMIT_ELEM, 'click', function (e) {
            e.preventDefault();
            e.stopImmediatePropagation();

            REVIEW_NAME_ELEM.innerHTML = [FIRST_NAME_ELEM.value, LAST_NAME_ELEM.value].join(' ');
            REVIEW_ADDRESS_ELEM.innerHTML = [HOUSE_ELEM.value, STREET_ELEM.value, TOWN_ELEM.value, POSTCODE_ELEM.value].join(', ');
            REVIEW_EMAIL_ELEM.innerHTML = EMAIL_ELEM.value;
        });

        bean.on(PAYMENT_DETAILS_SUBMIT_ELEM, 'click', function (e) {
            e.preventDefault();
            e.stopImmediatePropagation();

            REVIEW_ACCOUNT_ELEM.innerHTML = ACCOUNT_ELEM.value;
            REVIEW_SORTCODE_ELEM.innerHTML = [SORTCODE1_ELEM.value, SORTCODE2_ELEM.value, SORTCODE3_ELEM.value].join(', ');
            REVIEW_HOLDER_ELEM.innerHTML = HOLDER_ELEM.value;
        });
    };

    function init() {
        findAddress();
        manualAddress();
        reviewDetails();
    }

    return {
        init: init
    };

});
