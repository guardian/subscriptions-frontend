define(['$', 'modules/checkout/formElements'], function ($, form) {

    var ERROR_CLASS = 'form-field--error';

    var mandatoryFieldsPersonalDetails = [
        {input: form.$FIRST_NAME, container: form.$FIRST_NAME_CONTAINER},
        {input: form.$LAST_NAME, container: form.$LAST_NAME_CONTAINER},
        {input: form.$ADDRESS1, container: form.$ADDRESS1_CONTAINER},
        {input: form.$ADDRESS2, container: form.$ADDRESS2_CONTAINER},
        {input: form.$ADDRESS3, container: form.$ADDRESS3_CONTAINER},
        {input: form.$POSTCODE, container: form.$POSTCODE_CONTAINER}
    ];

    var mandatoryFieldsPaymentDetails = [
        {input: form.$ACCOUNT, container: form.$ACCOUNT_CONTAINER},
        {input: form.$SORTCODE1, container: form.$SORTCODE_CONTAINER},
        {input: form.$SORTCODE2, container: form.$SORTCODE_CONTAINER},
        {input: form.$SORTCODE3, container: form.$SORTCODE_CONTAINER},
        {input: form.$HOLDER, container: form.$HOLDER_CONTAINER}
    ];

    function toggleError(container, condition) {
        if (condition) {
            container.addClass(ERROR_CLASS);
        } else {
            container.removeClass(ERROR_CLASS);
        }
    }

    var validatePersonalDetails = function () {
        var emptyFields = mandatoryFieldsPersonalDetails.filter(function (field) {
            var isEmpty = field.input.val() == '';
            toggleError(field.container, isEmpty);
            return isEmpty;
        });
        var noEmptyFields = emptyFields.length == 0;

        var validEmail = form.$EMAIL.val().indexOf('@') > 0;
        toggleError(form.$EMAIL_CONTAINER, !validEmail);

        var emailCorrectTwice = form.$EMAIL.val() == form.$CONFIRM_EMAIL.val();
        toggleError(form.$CONFIRM_EMAIL_CONTAINER, validEmail && !emailCorrectTwice);

        return noEmptyFields && emailCorrectTwice && validEmail;
    };


    var validatePaymentDetails = function () {
        var emptyFields = mandatoryFieldsPaymentDetails.filter(function (field) {
            var isEmpty = field.input.val() == '';
            toggleError(field.container, isEmpty);
            return isEmpty;
        });
        var noEmptyFields = emptyFields.length == 0;


        var detailsConfirmed = form.$CONFIRM_PAYMENT[0].checked;
        toggleError(form.$CONFIRM_PAYMENT_CONTAINER, !detailsConfirmed);

        return noEmptyFields && detailsConfirmed;
    };

    return {
        validatePersonalDetails: validatePersonalDetails,
        validatePaymentDetails: validatePaymentDetails
    }

});