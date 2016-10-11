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
    var latestCurrencyAddressModel = null;
    var check = function(domEl) {
        $(domEl).attr('checked', 'checked');
        bean.fire(domEl, 'change');
    };
    var checkPlanInput = function (ratePlanId, currency) {
        ratePlanChoice.selectRatePlanForIdAndCurrency(ratePlanId, currency);
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

    var switchCurrency = function(model) {
        switchLocalization(model);
        checkPlanInput(model.ratePlanId, model.currency);
        selectPaymentMethod(model.country);
    };
    var initCurrencyOverride = function () {
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
        latestCurrencyAddressModel.currency = currency;
        switchCurrency(latestCurrencyAddressModel);
    };

    var everything = function(addressObject) {
        var $postcode = addressObject.$POSTCODE_CONTAINER,
            $subdivision = addressObject.$SUBDIVISION_CONTAINER,
            countryChoice = countryChoiceFunction(addressObject),
            shouldUpdateCurrency = addressObject.isCurrencyAddress();

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

        var redrawCountryOverride = function (selectedCurrency) {
            $('#currency-override-checkbox').attr('checked', false);

            var checkboxLabel = $('#currency-override-label');
            if (selectedCurrency == 'USD') {
                checkboxLabel.show();
            } else {
                checkboxLabel.hide();
            }
        };
        var redraw = function(model) {
            redrawAddressFields(model);
            if (shouldUpdateCurrency) {
                switchCurrency(model);
                redrawCountryOverride(model.currency);
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
            if (shouldUpdateCurrency) {
                latestCurrencyAddressModel= model;
            }
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
            initCurrencyOverride();

        }

    };
});
