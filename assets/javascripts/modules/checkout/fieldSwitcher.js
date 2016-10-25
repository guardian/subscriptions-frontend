/*global guardian*/
define([
    '$',
    'bean',
    'modules/checkout/countryChoice',
    'modules/checkout/addressFields',
    'modules/checkout/localizationSwitcher',
    'modules/checkout/formElements',
    'modules/checkout/ratePlanChoice'
], function ($, bean, countryChoiceFunction, addressFields, localizationSwitcher, formElements, ratePlanChoice) {
    'use strict';

    var currentState = {
        currencyOverrideChecked: false,
        deliveryAsBilling: true,
        localizationPrefix:null,
        localization: {}, //todo maybe later replace with a function that gets the correct settings based on the prefix
        delivery: {},
        billing: {}
    };
    //TODO come up with a better name or something

    var billingData = null;

    var check = function(domEl) {
        $(domEl).attr('checked', 'checked');
        bean.fire(domEl, 'change');
    };
    var checkPlanInput = function (ratePlanId, currency) {
        ratePlanChoice.selectRatePlanForIdAndCurrency(ratePlanId, currency);
    };

    // Change the payment method to Direct Debit for UK users,
    // Credit Card for international users
    var refreshPaymentMethods = function () {
        if (currentState.billing.country === 'GB' && currentState.localization.currency =='GBP') {
            check(formElements.$DIRECT_DEBIT_TYPE[0]);
        } else {
            check(formElements.$CARD_TYPE[0]);
        }
    };
    var isEmptyObject = function(obj) {
        return Object.keys(obj).length === 0 && obj.constructor === Object
    }
    var updateLocalization = function() {

        var updateParams = {};
        if(!isEmptyObject(currentState.delivery)) {
            updateParams['delivery-currency'] = currentState.delivery.currency;
            updateParams['delivery-country'] = currentState.delivery.country;
        }
        if(!isEmptyObject(currentState.billing)) {
            updateParams['billing-currency'] = currentState.billing.currency;
            updateParams['billing-country'] = currentState.billing.country;
        }
        if(!isEmptyObject(currentState.localization)) {
            updateParams['currency'] = currentState.localization.currency;
            updateParams['country'] = currentState.localization.country;
        }

        localizationSwitcher.refresh(updateParams);

        checkPlanInput(currentState.localization.ratePlanId, currentState.localization.currency);

        refreshPaymentMethods();


    };

    var setDeliveryAsBillingAddress = function (deliveryAsBilling) {
        currentState.deliveryAsBilling = deliveryAsBilling;

        if (deliveryAsBilling) {
            currentState.billing = currentState.delivery;
        }
        else {
            billingData.refresh();
        }
        updateLocalization()
    };

    var initCurrencyOverride = function () {
        $('.js-currency-override-checkbox').each(function (currencyOverrideCheckbox) {
            bean.on(currencyOverrideCheckbox, 'change', function () {
                currentState.currencyOverrideChecked = currencyOverrideCheckbox.checked;
                if (currentState.currencyOverrideChecked) {
                    currentState.localization.currency = 'GBP';
                }
                else {
                    currentState.localization.currency = 'USD';
                }
                updateLocalization();
                refreshPaymentMethods();
            });
        });
    };



    var redrawCurrencyOverride = function () {
        $('.js-currency-override-checkbox').attr('checked', false);

        var currencySelector = $('.js-currency-override-label, .js-currency-selector');

        if (currentState.localization.currency == 'USD') {
            currencySelector.show();
        } else {
            currencySelector.hide();
        }
    };

    var everything = function(addressObject, prefix) {
        var $postcode = addressObject.$POSTCODE_CONTAINER,
            $subdivision = addressObject.$SUBDIVISION_CONTAINER,
            countryChoice = countryChoiceFunction(addressObject),
            shouldUpdateMainLocalization = addressObject.determinesLocalization();


        var redrawAddressField = function($container, newField, modelValue) {

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

        var redrawAddressFields = function(model) {

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

        var redraw = function() {
            var model = currentState[prefix];
            redrawAddressFields(model);
            updateLocalization();
        };

        var getCurrentState = function() {
            var currentCountryOption = $(countryChoice.getCurrentCountryOption()),
                rules = countryChoice.addressRules(),
                currency = currentCountryOption.attr('data-currency-choice') || guardian.currency;


            return {
                postcode: $('input', $postcode).val(),
                postcodeRules: rules.postcode,
                subdivision: $('input', $subdivision).val(),
                subdivisionRules: rules.subdivision,
                currency: currency,
                country: currentCountryOption.val(),
                ratePlanId: ratePlanChoice.getSelectedRatePlanId()
            };
        };

        var updateGuardianPageInfo = function() {
            var pageInfo = guardian.pageInfo;
            if (pageInfo) {
                pageInfo.billingCountry = currentState.localization.country; // should this be billing country instead?
                pageInfo.billingCurrency = currentState.localization.currency;
            }
        };

        var refresh = function () {
            var model = getCurrentState();
            currentState[prefix] = model;
            if (shouldUpdateMainLocalization) {
                currentState.localization = model;
                currentState.localizationPrefix = prefix;
            }
            if (currentState.deliveryAsBilling && prefix == 'delivery') {
                currentState.billing = currentState.delivery;
            }
            redraw();
            if (shouldUpdateMainLocalization) {
                redrawCurrencyOverride();
            }
            updateGuardianPageInfo();
        };

        var refreshOnChange = function(el) {
            bean.on(el[0], 'change', function() {
                refresh();
            });
        };

        var init = function() {
            if (addressObject.$COUNTRY_SELECT.length) {
                countryChoice.preselectCountry(guardian.country);
                refreshOnChange(addressObject.$COUNTRY_SELECT);
                refresh();
            }
        };

        return {
            init: init,
            refresh: refresh
        };
    };

    return {
        init: function() {
           billingData = everything(formElements.BILLING, 'billing');
           billingData.init();
            everything(formElements.DELIVERY, 'delivery').init();
            initCurrencyOverride();
        },
        setDeliveryAsBillingAddress: setDeliveryAsBillingAddress
    };
});
