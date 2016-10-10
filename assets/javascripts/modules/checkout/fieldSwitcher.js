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

    var everything = function(addressObject) {
        var $postcode = addressObject.$POSTCODE_CONTAINER,
            $subdivision = addressObject.$SUBDIVISION_CONTAINER,
            countryChoice = countryChoiceFunction(addressObject),
            shouldUpdateCurrency = addressObject.isCurrencyAddress();


        var check = function(domEl) {
            $(domEl).attr('checked', 'checked');
            bean.fire(domEl, 'change');
        };

        var checkPlanInput = function (ratePlanId, currency) {
            ratePlanChoice.selectRatePlanForIdAndCurrency(ratePlanId, currency);
        };

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

        // Change the payment method to Direct Debit for UK users,
        // Credit Card for international users
        var selectPaymentMethod = function (country) {
            if (country === 'GB') {
                check(formElements.$DIRECT_DEBIT_TYPE[0]);
            } else {
                check(formElements.$CARD_TYPE[0]);
            }
        };

        var switchLocalization = function (model) {
            var currency = model.currency || guardian.currency;
            localizationSwitcher.set(currency, model.country);
        };
        var initCurrencyOverride = function () {
            //TODO SEE HOW TO GET THE DOM ELEMENT USING A $ SELECTOR AND CHECK IF I HAVE TO USE ONCLICK INSTEAD OF ONCHANGE FOR THE CHECKBOX
            var currencyOverrideCheckbox = document.getElementById('currency-override-checkbox');
            bean.on(currencyOverrideCheckbox, 'change', function () {
                if (currencyOverrideCheckbox.checked) {
                    overrideCurrency('GBP');
                }
                else {
                    overrideCurrency('USD');
                }
            });
        };
        var overrideCurrency = function(currency) {
            var model = getCurrentState();
            model.currency = currency;
            switchCurrency(model);
        };
        var switchCurrency = function(model) {
            switchLocalization(model);
            checkPlanInput(model.ratePlanId, model.currency);
            selectPaymentMethod(model.country);
        };
        var refreshCurrencyOverride = function (selectedCurrency) {
            var checkbox = $('#currency-override-label');
            if (selectedCurrency == 'USD') {
                checkbox.show();
            } else {
                checkbox.hide();
            }
        };
        var redraw = function(model) {
            redrawAddressFields(model);
            if (shouldUpdateCurrency) {
                switchCurrency(model);
                refreshCurrencyOverride(model.currency);
            }
        };

        var getCurrentState = function() {
            var currentCountryOption = $(countryChoice.getCurrentCountryOption()),
                rules = countryChoice.addressRules();

            return {
                postcode: $('input', $postcode).val(),
                postcodeRules: rules.postcode,
                subdivision: $('input', $subdivision).val(),
                subdivisionRules: rules.subdivision,
                currency: currentCountryOption.attr('data-currency-choice'),
                country: currentCountryOption.val(),
                ratePlanId: ratePlanChoice.getSelectedRatePlanId()
            };
        };

        var updateGuardianPageInfo = function(model) {
            var pageInfo = guardian.pageInfo;
            if (pageInfo) {
                pageInfo.billingCountry = model.country;
                pageInfo.billingCurrency = model.currency;
            }
        };

        var refresh = function () {
            var model = getCurrentState();
            redraw(model);
            updateGuardianPageInfo(model);
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
            initCurrencyOverride:initCurrencyOverride
        };
    };

    return {
        init: function() {
            everything(formElements.BILLING).init();
            everything(formElements.DELIVERY).init();
            //see what we can do about the useless parameter
            everything(formElements.BILLING).initCurrencyOverride();

        }

    };
});
