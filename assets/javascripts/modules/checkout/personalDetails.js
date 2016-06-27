define([
    '$',
    'modules/forms/toggleError',
    'modules/checkout/formElements',
    'modules/checkout/validatePersonal',
    'modules/forms/loader',
    'modules/checkout/fieldSwitcher',
    'modules/checkout/tracking'
], function (
    $,
    toggleError,
    formEls,
    validatePersonal,
    loader,
    fieldSwitcher,
    tracking
) {
    'use strict';

    var FIELDSET_COMPLETE = 'is-complete';
    var FIELDSET_COLLAPSED = 'is-collapsed';


    function requiredPersonalFields() {
        var result = [];
        $('.form-field', '#yourDetails').deepEach(function (el) {
            var input = $('input[required], select[required]', el);
            if (input.length) {
                result.push({input: input, container: $(el)});
            }
        });
        return result;
    }

    function requiredFieldVaues(fields) {
        return fields.map(function(field) {
            return field.input.val();
        });
    }

    function displayErrors(validity) {
        requiredPersonalFields().forEach(function(field) {
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

        formEls.$NOTICES.removeAttr('hidden');

        tracking.paymentDetailsTracking();
    }

    function handleValidation(personalDetails) {
        loader.setLoaderElem(document.querySelector('.js-personal-details-validating'));
        loader.startLoader();

        validatePersonal(
            personalDetails,
            guardian.user.isSignedIn
        ).then(function (validity) {
            loader.stopLoader();
            displayErrors(validity);
            if (validity.allValid) {
                nextStep();
            }
        });
    }

    function init() {
        fieldSwitcher.init();
        var $actionEl = formEls.$YOUR_DETAILS_SUBMIT;
        var actionEl = $actionEl[0];
        tracking.personalDetailsTracking();

        if ($actionEl.length) {
            actionEl.addEventListener('click', function(e) {
                e.preventDefault();
                handleValidation({
                    emailAddress: formEls.$EMAIL.val(),
                    emailAddressConfirmed: formEls.$CONFIRM_EMAIL.val(),
                    requiredFieldValues: requiredFieldVaues(requiredPersonalFields())
                });
            });
        }
    }

    return {
        init: init
    };

});
