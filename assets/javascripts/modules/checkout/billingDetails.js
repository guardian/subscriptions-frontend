define([
    'bean',
    'modules/forms/checkFields',
    'modules/checkout/formElements',
    'modules/checkout/eventTracking',
    'modules/checkout/reviewDetails',
    'modules/checkout/paymentDetails'
], function (
    bean,
    checkFields,
    formEls,
    eventTracking,
    reviewDetails,
    paymentDetails
) {
    'use strict';

    var FIELDSET_COMPLETE = 'is-complete';
    var FIELDSET_COLLAPSED = 'is-collapsed';

    function nextStep() {
        formEls.$FIELDSET_BILLING_ADDRESS
            .addClass(FIELDSET_COLLAPSED)
            .addClass(FIELDSET_COMPLETE);

        formEls.$FIELDSET_PAYMENT_DETAILS.removeClass(FIELDSET_COLLAPSED);
        formEls.$FIELDSET_PAYMENT_DETAILS[0].scrollIntoView();

        eventTracking.completedBillingDetails();
        reviewDetails.repopulateDetails();
        formEls.$CURRENCY_OVERRIDE_CHECKBOX.attr('disabled', true);

        //                                     you didn't see this \/
        if (formEls.$FIELDSET_PAYMENT_DETAILS[0].offsetHeight === 0) {
            paymentDetails.nextStep();
        }
    }

    function handleValidation() {
        if (!checkFields.checkRequiredFields(formEls.$FIELDSET_BILLING_ADDRESS)) {
            return;
        }
        nextStep();
    }

    function init() {
        var $actionEl = formEls.$BILLING_ADDRESS_SUBMIT;
        if ($actionEl.length) {
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
