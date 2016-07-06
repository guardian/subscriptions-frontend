define([
    '$',
    'modules/forms/toggleError',
    'modules/checkout/formElements',
    'modules/forms/checkFields',
    'modules/checkout/validatePersonal',
    'modules/forms/loader',
    'modules/checkout/fieldSwitcher',
    'modules/checkout/tracking'
], function (
    $,
    toggleError,
    formEls,
    checkFields,
    validatePersonal,
    loader,
    fieldSwitcher,
    tracking
) {
    'use strict';

    var FIELDSET_COMPLETE = 'is-complete';
    var FIELDSET_COLLAPSED = 'is-collapsed';


    function displayErrors(validity) {
        toggleError(formEls.$EMAIL_CONTAINER, !validity.hasValidEmail);
        toggleError(formEls.$CONFIRM_EMAIL_CONTAINER, validity.hasValidEmail && !validity.hasConfirmedEmail);
        toggleError(formEls.$EMAIL_CONTAINER, validity.isEmailInUse);

        if(validity.emailMessage) {
            formEls.$EMAIL_ERROR.text(validity.emailMessage);
            toggleError(formEls.$EMAIL_CONTAINER, true);
        }
    }

    function nextStep() {
        var fieldset = formEls.$FIELDSET_DELIVERY_DETAILS.length ?
            formEls.$FIELDSET_DELIVERY_DETAILS : formEls.$FIELDSET_BILLING_ADDRESS;

        fieldset.removeClass(FIELDSET_COLLAPSED);

        formEls.$FIELDSET_YOUR_DETAILS
            .addClass(FIELDSET_COLLAPSED)
            .addClass(FIELDSET_COMPLETE)[0]
            .scrollIntoView();

        formEls.$NOTICES.removeAttr('hidden');
        tracking.billingDetailsTracking();
    }

    function handleValidation(personalDetails) {
        
        var basicValidity = checkFields.checkRequiredFields(formEls.$FIELDSET_YOUR_DETAILS);
        loader.setLoaderElem(document.querySelector('.js-personal-details-validating'));
        loader.startLoader();

        validatePersonal(
            personalDetails,
            guardian.user.isSignedIn
        ).then(function (validity) {
            loader.stopLoader();
            displayErrors(validity);
            if (validity.allValid && basicValidity) {
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
                    emailAddressConfirmed: formEls.$CONFIRM_EMAIL.val()
                });
            });
        }
    }

    return {
        init: init
    };

});
