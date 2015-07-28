define([
    'modules/forms/toggleError',
    'modules/checkout/formElements',
    'modules/checkout/validatePersonal',
    '$'
], function (
    toggleError,
    formEls,
    validatePersonal,
    $
) {
    'use strict';

    var FIELDSET_COLLAPSED = 'fieldset--collapsed';
    var FIELDSET_COMPLETE = 'data-fieldset-complete';
    var HIDDEN_CLASS = 'is-hidden';

    var MAX_NAME_LENGTH = 50;
    var MAX_EMAIL_LENGTH = 240;
    var MAX_ADDRESS_LENGTH = 255;

    var ERROR_MESSAGES = {
        FIELD_TOO_LONG: 'This field is too long',
        REQUIRED_FIELD: 'This field is required',
        INVALID_POSTCODE: 'Please enter a valid postal code'
    };

    function TextField(containerEl, maxLength) {
        return {
            el: containerEl,
            maxLength: maxLength,
            input: $('.js-input', containerEl)[0],
            error: $('.js-error-message', containerEl)[0]
        };
    }

    var firstNameField = new TextField(formEls.$FIRST_NAME_CONTAINER, MAX_NAME_LENGTH);
    var lastNameField = new TextField(formEls.$LAST_NAME_CONTAINER, MAX_NAME_LENGTH);
    var emailField = new TextField(formEls.$EMAIL_CONTAINER, MAX_EMAIL_LENGTH);
    var address1Field = new TextField(formEls.$ADDRESS1_CONTAINER, MAX_ADDRESS_LENGTH);
    var address2Field = new TextField(formEls.$ADDRESS2_CONTAINER, MAX_ADDRESS_LENGTH);
    var address3Field = new TextField(formEls.$ADDRESS3_CONTAINER, MAX_ADDRESS_LENGTH);
    var postcodeField = new TextField(formEls.$POSTCODE_CONTAINER, MAX_ADDRESS_LENGTH);

    var requiredFields = [
        firstNameField,
        lastNameField,
        address1Field,
        address3Field,
        postcodeField
    ];

    var lengthCheckedFields = [
        firstNameField,
        lastNameField,
        emailField,
        address1Field,
        address2Field,
        address3Field,
        postcodeField
    ];

    function addError(field, message) {
        if (field === postcodeField) {
            field.error.textContent = ERROR_MESSAGES.INVALID_POSTCODE;
        } else {
            field.error.textContent = message;
        }
        toggleError(field.el, true);
    }

    function displayErrors(validity) {
        requiredFields.forEach(function(field) {
            if (!field.input.value) {
                addError(field, ERROR_MESSAGES.REQUIRED_FIELD);
            }
        });

        toggleError(formEls.$EMAIL_CONTAINER, !validity.hasValidEmail);
        toggleError(formEls.$CONFIRM_EMAIL_CONTAINER, validity.hasValidEmail && !validity.hasConfirmedEmail);
        toggleError(formEls.$EMAIL_CONTAINER, validity.isEmailInUse);

        if(validity.emailMessage) {
            formEls.$EMAIL_ERROR.text(validity.emailMessage);
            toggleError(formEls.$EMAIL_CONTAINER, true);
        }

        validity.fieldsTooLong.forEach(function(field) {
            addError(field, ERROR_MESSAGES.FIELD_TOO_LONG);
        });
    }

    function nextStep() {
        formEls.$FIELDSET_YOUR_DETAILS.addClass(FIELDSET_COLLAPSED).attr(FIELDSET_COMPLETE, '');
        formEls.$FIELDSET_PAYMENT_DETAILS.removeClass(FIELDSET_COLLAPSED);

        formEls.$EDIT_YOUR_DETAILS.addClass(HIDDEN_CLASS);
        formEls.$EDIT_PAYMENT_DETAILS.removeClass(HIDDEN_CLASS);
    }

    function handleValidation(personalDetails) {
        validatePersonal(personalDetails)
            .then(function (validity) {
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
                    requiredFields: requiredFields,
                    lengthCheckedFields: lengthCheckedFields
                });
            });
        }
    }

    return {
        init: init
    };

});
