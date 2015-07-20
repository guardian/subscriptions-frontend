define([
    '$',
    'utils/text',
    'modules/forms/regex',
    'modules/forms/toggleError',
    'modules/checkout/formElements',
    'modules/checkout/emailCheck'
], function (
    $,
    textUtils,
    regex,
    toggleError,
    form,
    emailCheck
) {
    'use strict';

    var MESSAGES = {
        emailInvalid: 'Please enter a valid email address.',
        emailTaken: 'Your email is already in use. Please sign in or use another email address.',
        emailFailure: 'There has been a problem. Please try again later.'
    };

    var mandatoryFieldsPersonalDetails = [
        {input: form.$FIRST_NAME, container: form.$FIRST_NAME_CONTAINER},
        {input: form.$LAST_NAME, container: form.$LAST_NAME_CONTAINER},
        {input: form.$ADDRESS1, container: form.$ADDRESS1_CONTAINER},
        {input: form.$ADDRESS3, container: form.$ADDRESS3_CONTAINER},
        {input: form.$POSTCODE, container: form.$POSTCODE_CONTAINER}
    ];

    function renderEmailError(condition, message){
        if(condition){
            form.$EMAIL_ERROR.text(message || MESSAGES.emailInvalid);
        }
    }

    function confirmBasicValidity(validity) {
        return !validity.emptyFields.length && validity.hasConfirmedEmail && validity.hasValidEmail;
    }

    function displayMandatoryValidityErrors(validity) {
        renderEmailError(!validity.hasValidEmail);
        toggleError(form.$EMAIL_CONTAINER, !validity.hasValidEmail);
        toggleError(form.$CONFIRM_EMAIL_CONTAINER, validity.hasValidEmail && !validity.hasConfirmedEmail);
    }

    /**
     * TODO:
     * Rather than querying DOM here, this should take
     * an object with the field data and then return an
     * object with validity data, this can then be unit
     * tested easily.
     */
    function validatePersonalDetails() {

        return new Promise(function (resolve, reject){

            var emailValue = form.$EMAIL.val();
            var emptyFields = mandatoryFieldsPersonalDetails.filter(function (field) {
                var isEmpty = !field.input.val();
                // TODO: Handle DOM/view code outside of promise
                toggleError(field.container, isEmpty);
                return isEmpty;
            });

            var validity = {
                allValid: false,
                emptyFields: emptyFields,
                hasValidEmail: regex.isValidEmail(emailValue),
                hasConfirmedEmail: form.$EMAIL.val() === form.$CONFIRM_EMAIL.val(),
                isEmailInUse: false
            };

            // TODO: Handle DOM/view code outside of promise
            displayMandatoryValidityErrors(validity);

            if(confirmBasicValidity(validity)) {
                if( guardian.user.isSignedIn ) {
                    validity.allValid = true;
                    resolve(validity);
                } else {
                    emailCheck(emailValue).then(function(isEmailInUse) {
                        if(isEmailInUse) {
                            validity.isEmailInUse = true;
                            // TODO: Handle DOM/view code outside of promise
                            renderEmailError(true, MESSAGES.emailTaken);
                            toggleError(form.$EMAIL_CONTAINER, true);
                            resolve(validity);
                        } else {
                            validity.allValid = true;
                            resolve(validity);
                        }
                    }).fail(function(err, msg) {
                        // TODO: Handle DOM/view code outside of promise
                        renderEmailError(true, MESSAGES.emailFailure);
                        reject(err, msg);
                    });
                }
            } else {
                resolve(validity);
            }
        });
    }

    var validatePaymentDetails = function (data) {

        var validity = {};

        validity.accountNumberValid = (
            data.accountNumber !== '' &&
            data.accountNumber.length >= 6 &&
            data.accountNumber.length <= 10 &&
            regex.isNumber(data.accountNumber)
        );

        validity.accountHolderNameValid = (
            data.accountHolderName !== '' &&
            data.accountHolderName.length <= 18
        );

        validity.sortCodeValid = data.sortCodeParts.filter(function (code) {
            var codeAsNumber = parseInt(code, 10);
            return (
                code.length === 2 &&
                codeAsNumber >= 0 && codeAsNumber <= 99
            );
        }).length === 3;

        validity.detailsConfirmedValid = data.detailsConfirmed;

        validity.allValid = (
            validity.accountNumberValid &&
            validity.accountHolderNameValid &&
            validity.sortCodeValid &&
            validity.detailsConfirmedValid
        );

        return validity;
    };

    return {
        validatePersonalDetails: validatePersonalDetails,
        validatePaymentDetails: validatePaymentDetails
    };

});
