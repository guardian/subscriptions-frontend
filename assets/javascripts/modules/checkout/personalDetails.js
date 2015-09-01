define([
    'modules/forms/toggleError',
    'modules/checkout/formElements',
    'modules/checkout/validatePersonal',
    'modules/checkout/tracking'
], function (
    toggleError,
    formEls,
    validatePersonal,
    tracking
) {
    'use strict';

    var FIELDSET_COMPLETE = 'is-complete';
    var FIELDSET_COLLAPSED = 'is-collapsed';

    var requiredFields = [
        {input: formEls.$FIRST_NAME, container: formEls.$FIRST_NAME_CONTAINER},
        {input: formEls.$LAST_NAME, container: formEls.$LAST_NAME_CONTAINER},
        {input: formEls.$ADDRESS1, container: formEls.$ADDRESS1_CONTAINER},
        {input: formEls.$ADDRESS3, container: formEls.$ADDRESS3_CONTAINER},
        {input: formEls.$POSTCODE, container: formEls.$POSTCODE_CONTAINER}
    ];

    function requiredFieldVaues(fields) {
        return fields.map(function(field) {
            return field.input.val();
        });
    }

    function displayErrors(validity) {

        requiredFields.forEach(function(field) {
            var isEmpty = !field.input.val();
            toggleError(field.container, isEmpty);
        });

        toggleError(formEls.$EMAIL_CONTAINER, !validity.hasValidEmail);
        toggleError(formEls.$CONFIRM_EMAIL_CONTAINER, validity.hasValidEmail && !validity.hasConfirmedEmail);
        toggleError(formEls.$EMAIL_CONTAINER, validity.isEmailInUse);

        if(validity.emailMessage) {
            formEls.$EMAIL_ERROR.text(validity.emailMessage);
            toggleError(formEls.$EMAIL_CONTAINER, true);
        }
    }

    function nextStep() {
        formEls.$FIELDSET_PAYMENT_DETAILS
            .removeClass(FIELDSET_COLLAPSED);

        formEls.$FIELDSET_YOUR_DETAILS
            .addClass(FIELDSET_COLLAPSED)
            .addClass(FIELDSET_COMPLETE)[0]
            .scrollIntoView();

        tracking.personalDetailsTracking();
    }

    function handleValidation(personalDetails) {
        validatePersonal(
            personalDetails,
            guardian.user.isSignedIn
        ).then(function (validity) {
            displayErrors(validity);
            if(validity.allValid) {
                nextStep();
            }
        });
    }

    function init() {
        var $actionEl = formEls.$YOUR_DETAILS_SUBMIT;
        var actionEl = $actionEl[0];

        if($actionEl.length) {
            actionEl.addEventListener('click', function(e) {
                e.preventDefault();
                handleValidation({
                    emailAddress: formEls.$EMAIL.val(),
                    emailAddressConfirmed: formEls.$CONFIRM_EMAIL.val(),
                    requiredFieldValues: requiredFieldVaues(requiredFields)
                });
            });
        }
    }

    return {
        init: init
    };

});
