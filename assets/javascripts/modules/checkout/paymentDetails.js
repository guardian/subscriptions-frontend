define([
    'bean',
    'utils/ajax',
    'modules/forms/toggleError',
    'modules/checkout/formElements',
    'modules/checkout/validatePayment'
], function (
    bean,
    ajax,
    toggleError,
    formEls,
    validatePayment
) {
    'use strict';

    var FIELDSET_COLLAPSED = 'is-collapsed';
    var FIELDSET_COMPLETE = 'is-complete';

    function displayErrors(validity) {
        toggleError(formEls.$ACCOUNT_CONTAINER, !validity.accountNumberValid);
        toggleError(formEls.$HOLDER_CONTAINER, !validity.accountHolderNameValid);
        toggleError(formEls.$SORTCODE_CONTAINER, !validity.sortCodeValid);
        toggleError(formEls.$CONFIRM_PAYMENT_CONTAINER, !validity.detailsConfirmedValid);
    }

    function nextStep() {
        formEls.$FIELDSET_PAYMENT_DETAILS
            .addClass(FIELDSET_COLLAPSED)
            .addClass(FIELDSET_COMPLETE);

        formEls.$FIELDSET_REVIEW
            .removeClass(FIELDSET_COLLAPSED);
    }

    function handleValidation() {
        var validity = validatePayment({
            accountNumber: formEls.$ACCOUNT.val(),
            accountHolderName: formEls.$HOLDER.val(),
            sortCodeParts: [
                formEls.$SORTCODE1.val(),
                formEls.$SORTCODE2.val(),
                formEls.$SORTCODE3.val()
            ],
            detailsConfirmed: formEls.$CONFIRM_PAYMENT[0].checked
        });
        displayErrors(validity);
        if(validity.allValid) {
            nextStep();
        }
    }

    function init() {
        var $actionEl = formEls.$PAYMENT_DETAILS_SUBMIT;
        if($actionEl.length) {
            bean.on($actionEl[0], 'click', function (evt) {
                evt.preventDefault();
                handleValidation();
            });
        }
    }

    return {
        init: init
    };

});
