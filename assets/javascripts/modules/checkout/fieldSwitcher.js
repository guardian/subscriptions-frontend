/*global guardian*/
define([
    '$',
    'bean',
    'modules/checkout/countryChoice',
    'modules/checkout/addressFields',
    'modules/checkout/dataSwitcher',
    'modules/checkout/formElements',
    'modules/checkout/ratePlanChoice',
    'modules/checkout/deliveryAsBilling'
], function ($, bean, countryChoiceFunction, addressFields, dataSwitcher, formElements, ratePlanChoice, deliveryAsBilling) {
    'use strict';

    var addressData = {
        billing : {},
        delivery: {},
        init : function() {
            this.billing = addressOps(formElements.BILLING, 'billing');
            this.delivery = addressOps(formElements.DELIVERY, 'delivery');
            this.billing.init();
            this.delivery.init();
            this.billing.refresh();
            this.delivery.refresh();
        }
    };

    var check = function (domEl) {
        $(domEl).attr('checked', 'checked');
        bean.fire(domEl, 'change');
    };

    var checkPlanInput = function (ratePlanId, currency) {
        ratePlanChoice.selectRatePlanForIdAndCurrency(ratePlanId, currency);
    };


    var refreshPaymentMethods = function (currentState) {
        if (currentState.billing.country === 'GB' && currentState.priceBanding.currency == 'GBP') {
            check(formElements.$DIRECT_DEBIT_TYPE[0]);
        } else {
            check(formElements.$CARD_TYPE[0]);
        }
    };

    var isEmptyObject = function (obj) {
       return (Object.keys(obj).length === 0 && obj.constructor === Object);
    };


    var updatePriceBanding = function (currentState) {
        var updateParams = {};

        var addFieldsFor = function (addressType) {
            var prefix = addressType == 'priceBanding' ? '' : addressType + '-';
            var values = currentState[addressType];
            if (!isEmptyObject(values)) {
                if (values.currency.length > 0) {
                    updateParams[prefix + 'currency'] = values.currency;
                }
                if (values.country.length > 0) {
                    updateParams[prefix + 'country'] = values.country;
                }
            }
        };

        addFieldsFor('delivery');
        addFieldsFor('billing');
        addFieldsFor('priceBanding');
        var selectedPlanBeforeUpdate = ratePlanChoice.getSelectedRatePlanId();
        dataSwitcher.refresh(updateParams);
        checkPlanInput(selectedPlanBeforeUpdate, currentState.priceBanding.currency);
        refreshPaymentMethods(currentState);
    };

    var getCurrentState = function () {
        var deliveryState = addressData.delivery.getState();
        var billingState = deliveryAsBilling.isEnabled() ? deliveryState : addressData.billing.getState();
        var priceBandingState = billingState.determinesPriceBanding ? billingState : deliveryState;

        if (isCurrencyOverrideChecked()) {
            priceBandingState.currency = 'GBP';
        }
        return {
            delivery: deliveryState,
            billing: billingState,
            priceBanding: priceBandingState
        }
    };
    var update = function () {
        var currentState = getCurrentState();
        updatePriceBanding(currentState)
    };

    var isCurrencyOverrideChecked = function() {
        var currencyOverrudeCheckbox = $('.js-currency-override-checkbox');
        return currencyOverrudeCheckbox.length >0 && currencyOverrudeCheckbox[0].checked;

    };

    var initCurrencyOverride = function () {
        $('.js-currency-override-checkbox').each(function (currencyOverrideCheckbox) {
            bean.on(currencyOverrideCheckbox, 'change', function () {
              update();
            });
        });
    };

    var redrawCurrencyOverride = function (currentState) {
        var currencySelector = $('.js-checkout-currency-override');

        if (currentState.priceBanding.currency == 'USD') {
            currencySelector.show();
        } else {
            currencySelector.hide();
        }
    };

    var addressOps = function (addressObject, prefix) {
        var $postcode = addressObject.$POSTCODE_CONTAINER,
            $subdivision = addressObject.$SUBDIVISION_CONTAINER,
            countryChoice = countryChoiceFunction(addressObject),
            determinesPriceBanding = addressObject.determinesPriceBanding();


        var redrawAddressField = function ($container, newField, modelValue) {

            $('.js-input', $container).replaceWith(newField.input);
            $('label', $container).replaceWith(newField.label);

            if (newField.label.textContent === '') {
                newField.input.value = '';
                $container.hide();
            } else {
                newField.input.value = modelValue;
                $container.show();
            }
        };

        var redrawAddressFields = function (model) {
            var newPostcode = addressFields.postcode(
                addressObject.getPostcode$().attr('name'),
                model.postcodeRules.required,
                model.postcodeRules.label);

            var newSubdivision = addressFields.subdivision(
                addressObject.getSubdivision$().attr('name'),
                model.subdivisionRules.required,
                model.subdivisionRules.label,
                model.subdivisionRules.values);

            redrawAddressField($postcode, newPostcode, model.postcode);
            redrawAddressField($subdivision, newSubdivision, model.subdivision);
        };


        var redraw = function (currentState) {
            var model = currentState[prefix];
            redrawAddressFields(model);
            updatePriceBanding(currentState);
        };

        var getState = function () {
            var currentCountryOption = $(countryChoice.getCurrentCountryOption()),
                rules = countryChoice.addressRules(),
                currency = currentCountryOption.attr('data-currency-choice') || guardian.currency;

            return {
                postcode: $('input', $postcode).val(),
                postcodeRules: rules.postcode,
                subdivision: $('input', $subdivision).val(),
                subdivisionRules: rules.subdivision,
                currency: currency,
                country: currentCountryOption.val()
            };
        };

        var refresh = function () {
            if (determinesPriceBanding) {
                $('.js-currency-override-checkbox').attr('checked', false);
            }
            
            var currentState = getCurrentState();

             if (determinesPriceBanding) {
                 redrawCurrencyOverride(currentState);
             }
           redraw(currentState);
        };

        var refreshOnChange = function (el) {
            bean.on(el[0], 'change', function () {
                refresh();
            });
        };

        var init = function () {
            if (addressObject.$COUNTRY_SELECT.length) {
                countryChoice.preselectCountry(guardian.country);
                refreshOnChange(addressObject.$COUNTRY_SELECT);
            }
        };

        return {
            init: init,
            getState: getState,
            determinesPriceBanding: determinesPriceBanding,
            refresh: refresh
        };
    };

    return {
        init: function () {
            addressData.init();
            initCurrencyOverride();
            update();
            deliveryAsBilling.registerOnChangeAction(update);

        },
        update: update
    };
});
