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

    var FIELDSET_COLLAPSED = 'fieldset--collapsed';
    var FIELDSET_COMPLETE = 'fieldset--complete';
    var FIELDSET_COMPLETE_ATTR = 'data-fieldset-complete';

    function displayErrors(validity) {
        toggleError(formEls.$ACCOUNT_CONTAINER, !validity.accountNumberValid);
        toggleError(formEls.$HOLDER_CONTAINER, !validity.accountHolderNameValid);
        toggleError(formEls.$SORTCODE_CONTAINER, !validity.sortCodeValid);
        toggleError(formEls.$CONFIRM_PAYMENT_CONTAINER, !validity.detailsConfirmedValid);
    }

    function nextStep() {
        formEls.$FIELDSET_PAYMENT_DETAILS
            .addClass(FIELDSET_COLLAPSED)
            .addClass(FIELDSET_COMPLETE)
            .attr(FIELDSET_COMPLETE_ATTR, '');

        formEls.$FIELDSET_REVIEW
            .removeClass(FIELDSET_COLLAPSED);
    }

    function handleValidation() {
        var validity = validatePayment({
            accountNumber: formEls.$ACCOUNT.val(),
            accountHolderName: formEls.$HOLDER.val(),
            sortCode: formEls.$SORTCODE.val(),
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
