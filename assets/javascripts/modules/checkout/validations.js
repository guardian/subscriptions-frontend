define([
    '$',
    'modules/checkout/form-elements',
    'modules/checkout/email-check',
    'modules/checkout/regex'
], function ($,
             form,
             emailCheck,
             regex) {
    'use strict';

    var ERROR_CLASS = 'form-field--error',
        EMAIL_ERROR_DEFAULT = 'Please enter a valid Email address',
        EMAIL_ERROR_TAKEN = 'Your email is already in use! Please sign in or use another email address.',
        EMAIL_ERROR_NETWORK = 'Your email could not be verified, please check your Internet connection and try again later.';

    var mandatoryFieldsPersonalDetails = [
        {input: form.$FIRST_NAME, container: form.$FIRST_NAME_CONTAINER},
        {input: form.$LAST_NAME, container: form.$LAST_NAME_CONTAINER},
        {input: form.$ADDRESS1, container: form.$ADDRESS1_CONTAINER},
        {input: form.$ADDRESS2, container: form.$ADDRESS2_CONTAINER},
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
            form.$EMAIL_ERROR.text(message || EMAIL_ERROR_DEFAULT);
        }
    }

    var validatePersonalDetails = function () {

        var staticValidations = new Promise(function (onSuccess, onFailure){
            var emptyFields = mandatoryFieldsPersonalDetails.filter(function (field) {
                var isEmpty = field.input.val() === '';
                toggleError(field.container, isEmpty);
                return isEmpty;
            });
            var noEmptyFields = emptyFields.length === 0;

            var validEmail = regex.isValidEmail(form.$EMAIL.val());
            renderEmailError(!validEmail);
            toggleError(form.$EMAIL_CONTAINER, !validEmail);

            var emailCorrectTwice = form.$EMAIL.val() === form.$CONFIRM_EMAIL.val();
            toggleError(form.$CONFIRM_EMAIL_CONTAINER, validEmail && !emailCorrectTwice);

            if(noEmptyFields && emailCorrectTwice && validEmail){
                onSuccess();
            }
            else {
                onFailure(new Error('Personal details incorrect.'));
            }
        });

        var validationResult = staticValidations.then(function(){

            var warnIfEmailTaken = emailCheck.warnIfEmailTaken();

            warnIfEmailTaken.catch(function (error) {
                if(error.message === 'EMAIL_IN_USE'){
                    renderEmailError(true, EMAIL_ERROR_TAKEN);
                }
                else {
                    renderEmailError(true, EMAIL_ERROR_NETWORK);
                }
                toggleError(form.$EMAIL_CONTAINER, true);

            });

            return warnIfEmailTaken
    });

        return validationResult;
    };


    var validatePaymentDetails = function () {
        var accountNumberValid = form.$ACCOUNT.val() !== ''
            && form.$ACCOUNT.val().length <= 10
            && regex.isNumber(form.$ACCOUNT.val());
        toggleError(form.$ACCOUNT_CONTAINER, !accountNumberValid);

        var holderNameValid = form.$HOLDER.val() !== ''
            && form.$HOLDER.val().length <= 18;
        toggleError(form.$HOLDER_CONTAINER, !holderNameValid);

        var sortCodeValid = [form.$SORTCODE1, form.$SORTCODE2, form.$SORTCODE3].filter(function (field) {
            var codeAsNumber = parseInt(field.val(), 10);
            var isValid = codeAsNumber >= 10 && codeAsNumber <= 99;
            return isValid;
        }).length === 3;
        toggleError(form.$SORTCODE_CONTAINER, !sortCodeValid);

        var detailsConfirmed = form.$CONFIRM_PAYMENT[0].checked;
        toggleError(form.$CONFIRM_PAYMENT_CONTAINER, !detailsConfirmed);

        return accountNumberValid && sortCodeValid && holderNameValid && detailsConfirmed;
    };

    return {
        validatePersonalDetails: validatePersonalDetails,
        validatePaymentDetails: validatePaymentDetails
    };

});
