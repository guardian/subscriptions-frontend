define([
    'modules/forms/checkFields',
    'modules/checkout/formElements',
    'modules/forms/toggleError',
    'utils/ajax',
    'bean',
    '$'
], function (
    checkFields,
    formEls,
    toggleError,
    ajax,
    bean,
    $
) {
    'use strict';

    var FIELDSET_COMPLETE = 'is-complete';
    var FIELDSET_COLLAPSED = 'is-collapsed';
    var ORIGINAL_ERROR_MESSAGE = '';
    var POSTCODE_ELIGIBLE = true;

    function handleBillingVisibility(e) {
        e.preventDefault();

        if (!POSTCODE_ELIGIBLE) {
            return;
        }
        if (!checkFields.checkRequiredFields(formEls.DELIVERY.$CONTAINER)) {
            return;
        }

        var $postcodeField = $('[name="delivery.postcode"]', formEls.DELIVERY.$POSTCODE_CONTAINER);
        if ($postcodeField.val().length === 1) {
            toggleError($postcodeField.parent(), true);
            return;
        }

        formEls.$FIELDSET_BILLING_ADDRESS.removeClass(FIELDSET_COLLAPSED);

        formEls.$FIELDSET_DELIVERY_DETAILS
            .addClass(FIELDSET_COLLAPSED)
            .addClass(FIELDSET_COMPLETE)[0]
            .scrollIntoView();

        formEls.$NOTICES.removeAttr('hidden');
    }

    function validatePostCode(e) {
        // Don't react on key presses that don't affect the text, or is just whitespace that'll get trimmed
        if (e.key     === 'Control' || e.key     === 'Shift' || e.key     === 'Alt' || e.key     ===  ' ' ||
            e.keyCode === 17        || e.keyCode === 16      || e.keyCode === 18    || e.keyCode === 32
        ) {
            return;
        }

        var $e = $(e.target),
            postCodeStr = $e.val().replace(/^\s+/, '').replace(/\s+/g, ' ').toUpperCase(); // trim only leading spaces

        $e.val(postCodeStr); // update display as soon as possible

        var parent = $e.parent(),
            $errorLabel = $('.js-error-message', parent),
            $deliveryPostcodeContainer = formEls.DELIVERY.$POSTCODE_CONTAINER,
            errorMessage = $deliveryPostcodeContainer.data('error-message'),
            validationUrl = $deliveryPostcodeContainer.data('validation-url');
            ORIGINAL_ERROR_MESSAGE = ORIGINAL_ERROR_MESSAGE || $errorLabel.text();

        // remove all whitespace for the lookup and trim to at most 4 characters for more user privacy
        postCodeStr = postCodeStr.replace(/\s+/g, '').substring(0, 4);

        if (postCodeStr.length < 2) {
            POSTCODE_ELIGIBLE = true;
            $errorLabel.text(ORIGINAL_ERROR_MESSAGE);
            toggleError(parent, false);
            return;
        }

        ajax({
            type: 'json',
            method: 'GET',
            url: validationUrl,
            data: {
                postCode: postCodeStr
            }
        }).then(function () {
            POSTCODE_ELIGIBLE = true;
            $errorLabel.text(ORIGINAL_ERROR_MESSAGE);
            toggleError(parent, false);
        }).catch(function () {
            POSTCODE_ELIGIBLE = false;
            $errorLabel.text(errorMessage);
            toggleError(parent, true);
        });
    }

    return {
        init: function() {

            if (!formEls.DELIVERY.$CONTAINER.length) {
                return;
            }

            bean.on(formEls.$DELIVERY_DETAILS_SUBMIT[0], 'click', handleBillingVisibility);

            if (formEls.DELIVERY.$POSTCODE_CONTAINER.data('validate-for') === 'delivery') {
                bean.on(formEls.DELIVERY.$POSTCODE_CONTAINER[0], 'change keyup', '.js-input', validatePostCode);
                bean.fire(formEls.DELIVERY.$POSTCODE_CONTAINER[0], 'change');
            }
        }
    };
});
