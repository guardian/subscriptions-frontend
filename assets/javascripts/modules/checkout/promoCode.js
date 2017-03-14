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

    var $inputBox         = formElements.$PROMO_CODE;
    var $ratePlanFields   = $('input[name="ratePlanId"]');
    var $promoCodeSnippet = $('.js-promo-code-snippet');
    var $promoCodeTsAndCs = $('.js-promo-code-tsandcs');
    var $promoCodeError   = $('.js-promo-code .js-error-message');
    var $promoCodeApplied = $('.js-promo-code-applied');
    var $promoRenew       = $('.js-promo-renew');

    $promoRenew.hide();
    $promoRenew.removeAttr('hidden');

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
        $promoRenew.hide();
        toggleError($promoCodeError.parent(), false);
        removePromotionFromRatePlans();
    }

    function displayError(message) {
        clearDown();
        message = message || 'Invalid promo code, please try again.';
        $promoCodeError.text(message);
        toggleError($promoCodeError.parent(), true);

    }
    function displayRenew() {
        clearDown();
        $promoRenew.show();

    }

    function displayPromotion(response, promoCode) {
        clearDown();
        $promoCodeApplied.show();
        $promoCodeSnippet.html(response.promotion.description);
        $promoCodeTsAndCs.attr('href', '/p/' + promoCode + '/terms');
        applyPromotionToRatePlans(response.adjustedRatePlans);
    }

    function isRetention(r){
        return ('promotion' in r) &&
        ('promotionType' in r.promotion) &&
        ((r.promotion.promotionType.name === 'retention') || (r.promotion.promotionType.a.name === 'retention') || (r.promotion.promotionType.b.name === 'retention'));
    }

    function validate() {
        var promoCode = $inputBox.val().trim(),
            ratePlanId = ratePlanChoice.getSelectedRatePlanId(),
            country = formElements.DELIVERY.$COUNTRY_SELECT.val().trim() || formElements.BILLING.$COUNTRY_SELECT.val().trim(),
            currency = document.querySelector('.js-rate-plans input:checked').dataset.currency;

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
        }).then(function (r)
        {
            if (isRetention(r)) {
                displayRenew();
            } else if (r.isValid) {
                displayPromotion(r, promoCode);
                bindExtraKeyListener();
            } else {
                displayError(r.errorMessage);
            }
        }).catch(function (r) {
            // Content of error codes are not parsed by ajax/reqwest.
            if (r && r.response) {
                var j = JSON.parse(r.response);
                if (j){
                    if(isRetention(j)){
                        displayRenew();
                        return;
                    } else if (j.errorMessage) {
                        displayError(j.errorMessage);
                        return;
                    }
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
