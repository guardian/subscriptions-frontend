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
    var $promotionalPlans = $('.js-promotional-plan');
    var selectedPlan      = ratePlanChoice.getSelectedRatePlanId();

    $promoRenew.hide();
    $promoRenew.removeAttr('hidden');

    var ratePlans = {};
    $ratePlanFields.each(function(el){
       ratePlans[el.value] = true;
    });

    function selectRatePlan(ratePlanId){
        var currency = ratePlanChoice.getSelectedOptionData().currency;
        ratePlanChoice.selectRatePlanForIdAndCurrency(ratePlanId, currency);
    }

    function containsRatePlans(r){
        return r.promotion.appliesTo.productRatePlanIds.map(function(rp){return !!ratePlans[rp]})
            .reduce(function(a,b){return a||b},false);
    }

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
        var selected = ratePlanChoice.getSelectedRatePlanId()
        $promotionalPlans.each(function(el){
            var $plan = $(el);
            $plan.attr('hidden',true);
            var ratePlanId = el.querySelector('input').value;
            if(ratePlanId===selected){
                selectRatePlan(selectedPlan);
            }
        });

        $promotionalPlans.attr('hidden',true);
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
        $promotionalPlans.each(function(el){
            var ratePlanId = el.querySelector('input').value;
            if (response.promotion.appliesTo.productRatePlanIds.indexOf(ratePlanId)!==-1) {
                el.removeAttribute('hidden');
                selectRatePlan(ratePlanId);
            }
        });
        applyPromotionToRatePlans(response.adjustedRatePlans);
    }


    function isRetention(r) {
        if(r.promotion == null || r.promotion.promotionType == null){
            return false;
        }
        if('a' in r.promotion.promotionType){
            return (r.promotion.promotionType.a.name === 'retention') || (r.promotion.promotionType.b.name === 'retention');
        }
        return (r.promotion.promotionType.name === 'retention');
    }

    function validate() {
        var promoCode = $inputBox.val().trim(),
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
            url: jsRoutes.controllers.Promotion.validate(promoCode, country, currency).url
        }).then(function (r)
        {
            if (isRetention(r)) {
                displayRenew();
                return;
            }
            if (r.isValid) {
                if (containsRatePlans(r)) {
                    displayPromotion(r, promoCode);
                    bindExtraKeyListener();
                    return;
                }
                else {
                    displayError('The promo code you supplied is not applicable for this product');
                }
            }
            displayError(r.errorMessage);
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
