define([
    'bean',
    'utils/ajax',
    'modules/forms/toggleError',
    'modules/checkout/formElements',
    'modules/checkout/displayCardImg',
    'modules/checkout/payment',
    'modules/checkout/eventTracking',
    'modules/checkout/impressionTracking',
    'lodash/collection/find',
    'modules/forms/loader',
    'lodash/object/assign'
], function (
    bean,
    ajax,
    toggleError,
    formEls,
    displayCardImg,
    payment,
    eventTracking,
    impressionTracking,
    find,
    loader,
    assign
) {
    'use strict';

    var FIELDSET_COMPLETE = 'is-complete';
    var FIELDSET_COLLAPSED = 'is-collapsed';


    function displayErrors(validity) {
        toggleError(formEls.$ACCOUNT_CONTAINER, !validity.accountNumberValid);
        toggleError(formEls.$HOLDER_CONTAINER, !validity.accountHolderNameValid);
        toggleError(formEls.$SORTCODE_CONTAINER, !validity.sortCodeValid);
        toggleError(formEls.$CONFIRM_PAYMENT_CONTAINER, !validity.detailsConfirmedValid);

        toggleError(formEls.$CARD_NUMBER_CONTAINER, !validity.cardNumberValid);
        toggleError(formEls.$CARD_CVC_CONTAINER, !validity.cardCVCValid);
        toggleError(formEls.$CARD_EXPIRY_CONTAINER, !validity.cardExpiryValid);
    }

    function nextStep() {
        formEls.$FIELDSET_PAYMENT_DETAILS
            .addClass(FIELDSET_COLLAPSED)
            .addClass(FIELDSET_COMPLETE);

        formEls.$FIELDSET_REVIEW
            .removeClass(FIELDSET_COLLAPSED);

        formEls.$FIELDSET_YOUR_DETAILS[0]
            .scrollIntoView();

        eventTracking.completedPaymentDetails();
        impressionTracking.paymentReviewTracking();
    }

    function handleValidation() {
        var paymentMethod = find(formEls.$PAYMENT_METHOD, function(elem) {return elem.checked}).value;
        var paymentDetails = {paymentMethod: paymentMethod};

        if (paymentMethod === 'direct-debit') {
            assign(paymentDetails, {
                accountNumber: formEls.$ACCOUNT.val(),
                accountHolderName: formEls.$HOLDER.val(),
                sortCode: formEls.$SORTCODE.val(),
                detailsConfirmed: formEls.$CONFIRM_PAYMENT[0].checked
            });
        } else if (paymentMethod === 'card') {
            toggleError(formEls.$CARD_CONTAINER, false);
            assign(paymentDetails, {
                cardNumber: formEls.$CARD_NUMBER.val(),
                cardCVC: formEls.$CARD_CVC.val(),
                cardExpiryMonth: formEls.$CARD_EXPIRY_MONTH.val(),
                cardExpiryYear: formEls.$CARD_EXPIRY_YEAR.val()
            });
        } else {
            throw new Error('Invalid payment method '+paymentMethod);
        }

        loader.setLoaderElem(document.querySelector('.js-payment-details-validating'));
        loader.startLoader();
        payment.validate(paymentDetails).then(function(validity) {
            loader.stopLoader();
            displayErrors(validity);
            if (validity.allValid) {
                nextStep();
            }
        });
    }

    function init() {
        var $actionEl = formEls.$PAYMENT_DETAILS_SUBMIT;
        if ($actionEl.length) {
            bean.on($actionEl[0], 'click', function (evt) {
                evt.preventDefault();
                handleValidation();
            });
        }

        var $cardNumberEl = formEls.$CARD_NUMBER;
        if ($cardNumberEl.length) {
            bean.on($cardNumberEl[0], 'keyup blur', function (e) {
                var input = e && e.target;
                displayCardImg(input.value);
            });
        }
    }

    return {
        init: init
    };

});
