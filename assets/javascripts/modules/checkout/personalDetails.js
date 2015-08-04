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
    var FIELDSET_COMPLETE = 'fieldset--complete';
    var FIELDSET_COMPLETE_ATTR = 'data-fieldset-complete';

    var MAX_NAME_LENGTH = 50;
    var MAX_EMAIL_LENGTH = 240;
    var MAX_ADDRESS_LENGTH = 255;

    var ERROR_MESSAGES = {
        FIELD_TOO_LONG: 'This field is too long',
        REQUIRED_FIELD: 'This field is required',
        INVALID_POSTCODE: 'Please enter a valid postal code'
    };

    function textField(containerEl, maxLength) {
        return {
            el: containerEl,
            maxLength: maxLength,
            input: $('.js-input', containerEl)[0],
            error: $('.js-error-message', containerEl)[0]
        };
    }

    var firstNameField = textField(formEls.$FIRST_NAME_CONTAINER, MAX_NAME_LENGTH);
    var lastNameField = textField(formEls.$LAST_NAME_CONTAINER, MAX_NAME_LENGTH);
    var address1Field = textField(formEls.$ADDRESS1_CONTAINER, MAX_ADDRESS_LENGTH);
    var address3Field = textField(formEls.$ADDRESS3_CONTAINER, MAX_ADDRESS_LENGTH);
    var postcodeField = textField(formEls.$POSTCODE_CONTAINER, MAX_ADDRESS_LENGTH);

    var requiredFields = [
        firstNameField,
        lastNameField,
        address1Field,
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
        formEls.$FIELDSET_YOUR_DETAILS
            .addClass(FIELDSET_COLLAPSED)
            .addClass(FIELDSET_COMPLETE)
            .attr(FIELDSET_COMPLETE_ATTR, '');

        formEls.$FIELDSET_PAYMENT_DETAILS
            .removeClass(FIELDSET_COLLAPSED);
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
                    lengthCheckedFields: [
                        textField(formEls.$FIRST_NAME_CONTAINER, MAX_NAME_LENGTH),
                        textField(formEls.$LAST_NAME_CONTAINER, MAX_NAME_LENGTH),
                        textField(formEls.$EMAIL_CONTAINER, MAX_EMAIL_LENGTH),
                        textField(formEls.$ADDRESS1_CONTAINER, MAX_ADDRESS_LENGTH),
                        textField(formEls.$ADDRESS2_CONTAINER, MAX_ADDRESS_LENGTH),
                        textField(formEls.$ADDRESS3_CONTAINER, MAX_ADDRESS_LENGTH),
                        textField(formEls.$POSTCODE_CONTAINER, MAX_ADDRESS_LENGTH)
                    ]
                });
            });
        }
    }

    return {
        init: init
    };

});
