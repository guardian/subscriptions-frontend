define([
    '$',
    'bean',
    'modules/forms/toggleError',
    'utils/ajax',
    'modules/checkout/formElements',
    'modules/checkout/countryChoice'
], function ($, bean, toggleError, ajax, formElements, countryChoice) {
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
                newDisplayPrice = $el.data('option-mirror-label-default');

            $label.html(newDisplayPrice);
            $el.attr('data-option-mirror-label', newDisplayPrice);
            if ($el.attr('checked')) {
                bean.fire(el, 'change');
            }
        });
    }

    function formatMoney(currencyIdentifier, amount) {
        // rounds any remainder up tp 1p so we always quote a higher price than the user might eventually get charged.
        var price2dp = Math.ceil(amount * 100) / 100,
            priceNoDot00 = price2dp.toFixed(2).replace(/\.00$/, '');

        return currencyIdentifier + priceNoDot00;
    }

    function applyPromotionToRatePlans(promotion) {
        if (promotion.promotionType.amount > 0) {
            $ratePlanFields.each(function(el) {
                var $el = $(el),
                    amount = Number($el.data('amount-in-currency')),
                    currency = $el.data('currency'),
                    currencyIdentifier = $el.data('currency-identifier'),
                    discountPercent = promotion.promotionType.amount,
                    frequency = $el.data('frequency'),
                    $label = $('#label-for-' + $el.val() + '-' + currency),
                    newDisplayPrice = formatMoney(currencyIdentifier, amount * (1 - (discountPercent / 100))) + ' ' + frequency;

                $label.html(newDisplayPrice);
                $el.attr('data-option-mirror-label', newDisplayPrice);
                if ($el.attr('checked')) {
                    bean.fire(el, 'change');
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

    function displayPromotion(promotion) {
        clearDown();
        $promoCodeApplied.show();
        $promoCodeSnippet.html(promotion.description);
        applyPromotionToRatePlans(promotion);
    }

    function validate() {
        var promoCode  = $inputBox.val().trim();
        if (promoCode === '') {
            clearDown();
            return;
        }

        ajax({
            type: 'json',
            method: 'GET',
            url: lookupUrl,
            data: {
                promoCode: promoCode,
                productRatePlanId: formElements.getRatePlanId(),
                country: countryChoice.getCurrentCountryOption().value
            }
        }).then(function (a) {
            if (a.isValid) {
                displayPromotion(a.promotion);
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
            var $countrySelectBox = formElements.$COUNTRY_SELECT,
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
