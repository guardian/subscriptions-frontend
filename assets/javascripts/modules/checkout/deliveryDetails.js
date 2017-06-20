define([
    'modules/checkout/eventTracking',
    'modules/forms/checkFields',
    'modules/checkout/formElements',
    'modules/forms/toggleError',
    'utils/ajax',
    'bean',
    '$'
], function (
    eventTracking,
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

    var M25_POSTCODE_PREFIXES = [
        'BR1', 'BR2', 'BR3', 'BR4', 'BR5', 'BR6', 'BR7', 'BR8',
        'CM14',
        'CR0', 'CR2', 'CR3', 'CR4', 'CR5', 'CR6', 'CR7', 'CR8', 'CR9',
        'DA1', 'DA5', 'DA6', 'DA7', 'DA8',
        'E1', 'E2', 'E3', 'E4', 'E5', 'E6', 'E7', 'E8', 'E9',
        'EC1', 'EC2', 'EC3', 'EC4',
        'EN1', 'EN2', 'EN3', 'EN4', 'EN5',
        'HA0', 'HA1', 'HA2', 'HA3', 'HA4', 'HA5', 'HA6', 'HA7', 'HA8', 'HA9',
        'IG1', 'IG2', 'IG3', 'IG5', 'IG6', 'IG7', 'IG8', 'IG9',
        'KT1', 'KT2', 'KT3', 'KT4', 'KT5', 'KT6', 'KT7', 'KT8', 'KT9',
        'N1', 'N2', 'N3', 'N4', 'N5', 'N6', 'N7', 'N8', 'N9',
        'NW1', 'NW2', 'NW3', 'NW4', 'NW5', 'NW6', 'NW7', 'NW8', 'NW9',
        'RM1', 'RM2', 'RM3', 'RM4', 'RM5', 'RM6', 'RM7', 'RM8', 'RM9',
        'SE1', 'SE2', 'SE3', 'SE4', 'SE5', 'SE6', 'SE7', 'SE8', 'SE9',
        'SM1', 'SM2', 'SM3', 'SM4', 'SM5', 'SM6', 'SM7',
        'SW1', 'SW2', 'SW3', 'SW4', 'SW5', 'SW6', 'SW7', 'SW8', 'SW9',
        'TN16',
        'TW1', 'TW2', 'TW3', 'TW4', 'TW5', 'TW6', 'TW7', 'TW8', 'TW9',
        'UB10', 'UB3', 'UB4', 'UB5', 'UB6',
        'W1', 'W2', 'W3', 'W4', 'W5', 'W6', 'W7', 'W8', 'W9',
        'WC1A','WC1E', 'WC2',
        'WD1', 'WD2', 'WD3', 'WD4', 'WD5', 'WD6', 'WD7'
    ];

    function isInsideM25(needle) {
        var i,
            leni = M25_POSTCODE_PREFIXES.length,
            prefix;

        for (i = 0; i < leni; i += 1) {
            prefix = M25_POSTCODE_PREFIXES[i];
            if (needle.startsWith(prefix) || prefix.startsWith(needle)) {
                return true;
            }
        }
        return false;
    }

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

        eventTracking.completedDeliveryDetails();
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
            errorMessage = $deliveryPostcodeContainer.data('error-message');

        ORIGINAL_ERROR_MESSAGE = ORIGINAL_ERROR_MESSAGE || $errorLabel.text();

        postCodeStr = postCodeStr.replace(/\s+/g, '');

        if (postCodeStr.length > 0) {
            POSTCODE_ELIGIBLE = isInsideM25(postCodeStr);
        } else {
            POSTCODE_ELIGIBLE = true;  // assume it's valid if it's ineligible for lookup
        }

        if (POSTCODE_ELIGIBLE) {
            $errorLabel.text(ORIGINAL_ERROR_MESSAGE);
            toggleError(parent, false);
        } else {
            $errorLabel.text(errorMessage);
            toggleError(parent, true);
        }
    }

    return {
        init: function () {

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
