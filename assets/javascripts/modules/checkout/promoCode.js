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

    function displayError(message) {
        message = message || 'Invalid promo code, please try again.';
        $promoCodeSnippet.html('');
        $promoCodeApplied.hide();
        $promoCodeError.text(message);
        toggleError($promoCodeError.parent(), true);
    }

    function displayPromotion(promotion) {
        toggleError($promoCodeError.parent(), false);
        $promoCodeError.text('');
        $promoCodeApplied.show();
        $promoCodeSnippet.html(promotion.description);
    }

    function validate() {
        var promoCode  = $inputBox.val().trim();
        if (promoCode == '') {
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
            bean.on(formElements.$COUNTRY_SELECT[0], 'change', validate);
            bean.on(formElements.$PROMO_CODE_BTN[0], 'click', validate);
            if ($inputBox.val() != '') {
                validate();
            }
        }
    };
});
