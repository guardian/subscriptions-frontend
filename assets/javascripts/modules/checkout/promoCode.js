/* global jsRoutes */
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
        $promoCodeTsAndCs   = $('.js-promo-code-tsandcs'),
        $promoCodeError     = $('.js-promo-code .js-error-message'),
        $promoCodeApplied   = $('.js-promo-code-applied');

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
        $promoCodeTsAndCs.attr('href', '#');
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

    function displayPromotion(response, promoCode) {
        clearDown();
        $promoCodeApplied.show();
        $promoCodeSnippet.html(response.promotion.description);
        $promoCodeTsAndCs.attr('href', '/p/' + promoCode + '/terms');
        applyPromotionToRatePlans(response.adjustedRatePlans);
    }

    function validate() {
        var promoCode = $inputBox.val().trim(),
            ratePlanId = ratePlanChoice.getSelectedRatePlanId(),
            country = formElements.DELIVERY.$COUNTRY_SELECT.val().trim() || formElements.BILLING.$COUNTRY_SELECT.val().trim(),
            selectedPlan = document.querySelector('.js-rate-plans input:checked'),
            currency = selectedPlan === null ? null : selectedPlan.dataset.currency;

        if (promoCode === '') {
            clearDown();
            return;
        }
        if (country === '') {
            displayError('Please choose a billing or delivery country to validate this promo code');
        }

        ajax({
            type: 'json',
            method: 'GET',
            url: jsRoutes.controllers.Promotion.validateForProductRatePlan(promoCode, ratePlanId, country, currency).url
        }).then(function (a) {
            if (a.isValid) {
                displayPromotion(a, promoCode);
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
            var changeListeners = [
                    formElements.DELIVERY.$COUNTRY_SELECT,
                    formElements.BILLING.$COUNTRY_SELECT,
                    formElements.$CURRENCY_OVERRIDE_CHECKBOX
                ],
                clickListeners = [
                    formElements.$PROMO_CODE_BTN
                ];

            changeListeners.forEach(function ($el) {
                if ($el.length) {
                    bean.on($el[0], 'change', validate);
                }
            });
            clickListeners.forEach(function ($el) {
                if ($el.length) {
                    bean.on($el[0], 'click', validate);
                }
            });

            if ($inputBox.val() !== '') {
                validate();
            }
        }
    };
});
