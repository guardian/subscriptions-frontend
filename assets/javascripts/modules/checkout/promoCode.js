define([
    '$',
    'bean',
    'modules/forms/toggleError',
    'utils/ajax',
    'modules/checkout/formElements',
    'modules/checkout/ratePlanChoice'
], function ($, bean, toggleError, ajax, formElements, ratePlanChoice) {
    'use strict';

    var $inputBox           = formElements.$PROMO_CODE,
        $ratePlanFields     = $('input[name="ratePlanId"]'),
        $promoCodeSnippet   = $('.js-promo-code-snippet'),
        $promoCodeError     = $('.js-promo-code .js-error-message'),
        $promoCodeApplied   = $('.js-promo-code-applied'),
        lookupUrl           = $inputBox.data('lookup-url');

    function bindExtraKeyListener() {
        if (bindExtraKeyListener.alreadyBound) {
            return;
        }
        bean.on($inputBox[0], 'keyup blur', validate);
        bindExtraKeyListener.alreadyBound = true;
    }

    function removePromotionFromRatePlans() {
        $ratePlanFields.each(function(el) {
            var $el = $(el),
                currency = $el.data('currency'),
                $label = $('#label-for-' + $el.val() + '-' + currency),
                labelPrefix = $el.data('option-label-prefix'),
                newDisplayPrice = $el.data('option-mirror-payment-default');

            $label.html(labelPrefix + newDisplayPrice);
            $el.attr('data-option-mirror-payment', newDisplayPrice);
            if ($el.attr('checked')) {
                bean.fire(el, 'change');
            }
        });
    }

    function applyPromotionToRatePlans(adjustedRatePlans) {
        if (adjustedRatePlans) {
            $ratePlanFields.each(function(el) {
                var $el = $(el),
                    ratePlanId = $el.val(),
                    currency = $el.data('currency'),
                    labelPrefix = $el.data('option-label-prefix'),
                    $label = $('#label-for-' + ratePlanId + '-' + currency);

                if (adjustedRatePlans[ratePlanId]) {
                    $label.html(labelPrefix + adjustedRatePlans[ratePlanId]);
                    $el.attr('data-option-mirror-payment', adjustedRatePlans[ratePlanId]);
                    if ($el.attr('checked')) {
                        bean.fire(el, 'change');
                    }
                }
            });
        } else {
            removePromotionFromRatePlans();
        }
    }

    function clearDown() {
        $promoCodeError.text('');
        $promoCodeSnippet.html('');
        $promoCodeApplied.hide();
        toggleError($promoCodeError.parent(), false);
        removePromotionFromRatePlans();
    }

    function displayError(message) {
        clearDown();
        message = message || 'Invalid promo code, please try again.';
        $promoCodeError.text(message);
        toggleError($promoCodeError.parent(), true);

    }

    function displayPromotion(response) {
        clearDown();
        $promoCodeApplied.show();
        $promoCodeSnippet.html(response.promotion.description);
        applyPromotionToRatePlans(response.adjustedRatePlans);
    }

    function validate() {
        var country = formElements.BILLING.$COUNTRY_SELECT.val().trim(),
            promoCode = $inputBox.val().trim();

        if (country === '' || promoCode === '') {
            clearDown();
            return;
        }

        ajax({
            type: 'json',
            method: 'GET',
            url: lookupUrl,
            data: {
                promoCode: promoCode,
                productRatePlanId: ratePlanChoice.getSelectedRatePlanId(),
                country: country
            }
        }).then(function (a) {
            if (a.isValid) {
                displayPromotion(a);
                bindExtraKeyListener();
            } else {
                displayError(a.errorMessage);
            }
        }).catch(function (a) {
            // Content of error codes are not parsed by ajax/reqwest.
            if (a && a.response) {
                var b = JSON.parse(a.response);
                if (b && b.errorMessage) {
                    displayError(b.errorMessage);
                    return;
                }
            }
            displayError();
        });
    }

    return {
        init: function () {
            var $countrySelectBox = formElements.BILLING.$COUNTRY_SELECT,
                $promoCodeButton = formElements.$PROMO_CODE_BTN;

            if ($countrySelectBox.length && $promoCodeButton.length) {
                bean.on($countrySelectBox[0], 'change', validate);
                bean.on($promoCodeButton[0], 'click', validate);
                if ($inputBox.val() !== '') {
                    validate();
                }
            }
        }
    };
});
