define(['$', 'bean'], function ($, bean) {
    'use strict';

    var _ON_CHANGE_ACTIONS = [];

    function forEveryPlanOption(fn) {
        // No caching due to possibility of other modules affecting the list of available options
        // e.g we may one say switch the UI between voucher plans and delivery plans depending on
        // the postcode a customer provides.
        $('.js-rate-plans input[type="radio"]').each(fn);
    }

    function getSelectedOptionData() {
        var data = null;
        forEveryPlanOption(function (option) {
            if ($(option).attr('checked')) {
                data = $(option).data();
            }
        });
        return data;
    }

    function getSelectedRatePlanId() {
        // Bonzo has no filter function :(
        var ratePlanId = null;
        forEveryPlanOption(function (option) {
            if ($(option).attr('checked')) {
                ratePlanId = $(option).val();
            }
        });
        return ratePlanId;
    }

    function getSelectedRatePlanName() {
        // Bonzo has no filter function :(
        var packageName = null;
        forEveryPlanOption(function (option) {
            if ($(option).attr('checked')) {
                packageName = $(option).attr('data-name');
            }
        });
        return packageName;
    }

    function selectRatePlanForIdAndCurrency(ratePlanId, currency) {
        forEveryPlanOption(function(option) {
            if ($(option).val() === ratePlanId && $(option).attr('data-currency') === currency) {
                $(option).attr('checked', 'checked');
                bean.fire(option, 'change');
            }
        });
    }

    function fireAllOnChangeActions() {
        _ON_CHANGE_ACTIONS.forEach(function (fn) {
            if (typeof(fn) === 'function') {
                fn();
            }
        });
    }

    function addOnChangeAction(fn) {
        console.log('add', fn);
        if (typeof(fn) === 'function') {
            _ON_CHANGE_ACTIONS.push(fn);
        }
    }

    return {
        getSelectedRatePlanId: getSelectedRatePlanId,
        getSelectedRatePlanName: getSelectedRatePlanName,
        getSelectedOptionData: getSelectedOptionData,
        selectRatePlanForIdAndCurrency: selectRatePlanForIdAndCurrency,
        registerOnChangeAction: addOnChangeAction,
        init: function () {
            forEveryPlanOption(function (option) {
                bean.on(option, 'change', fireAllOnChangeActions);
            });
        }
    }

});
