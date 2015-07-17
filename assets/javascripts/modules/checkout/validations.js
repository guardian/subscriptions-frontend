define([
    '$',
    'utils/text',
    'modules/checkout/formElements',
    'modules/checkout/emailCheck',
    'modules/checkout/regex'
], function (
    $,
    textUtils,
    form,
    emailCheck,
    regex
) {
    'use strict';

    var ERROR_CLASS = 'form-field--error';
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

    function toggleError(container, condition) {
        if (condition) {
            container.addClass(ERROR_CLASS);
        } else {
            container.removeClass(ERROR_CLASS);
        }
    }

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

    var validatePaymentDetails = function () {
        var accountNumberValid = form.$ACCOUNT.val() !== ''
            && form.$ACCOUNT.val().length >= 6
            && form.$ACCOUNT.val().length <= 10
            && regex.isNumber(form.$ACCOUNT.val());
        toggleError(form.$ACCOUNT_CONTAINER, !accountNumberValid);

        var holderNameValid = form.$HOLDER.val() !== ''
            && form.$HOLDER.val().length <= 18;
        toggleError(form.$HOLDER_CONTAINER, !holderNameValid);

        var sortCodeValid = [form.$SORTCODE1, form.$SORTCODE2, form.$SORTCODE3].filter(function (field) {
            var codeAsNumber = parseInt(field.val(), 10);
            var isValid = field.val().length === 2
                && codeAsNumber >= 0 && codeAsNumber <= 99;
            return isValid;
        }).length === 3;
        toggleError(form.$SORTCODE_CONTAINER, !sortCodeValid);

        var detailsConfirmed = form.$CONFIRM_PAYMENT[0].checked;
        toggleError(form.$CONFIRM_PAYMENT_CONTAINER, !detailsConfirmed);

        return accountNumberValid && sortCodeValid && holderNameValid && detailsConfirmed;
    };

    var validateFinishAccount = function () {
        var passwordValid = form.$FINISH_ACCOUNT_PASSWORD.val().length >= 6;
        toggleError(form.$FINISH_ACCOUNT_PASSWORD_CONTAINER, !passwordValid);
        return passwordValid;
    };

    return {
        validateFinishAccount: validateFinishAccount,
        validatePersonalDetails: validatePersonalDetails,
        validatePaymentDetails: validatePaymentDetails
    };

});
