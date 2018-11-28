define(['modules/checkout/formElements','$', 'bean'], function (formEls, $, bean) {
    'use strict';

    var BILLING_ADDRESS_AS_DELIVERY_ADDRESS_PICKER = $('.js-checkout-delivery-same-as-billing')[0];
    var $BILLING_ADDRESS = formEls.BILLING.$CONTAINER;

    var _ON_CHANGE_ACTIONS = [];

    function fireAllOnChangeActions() {
        _ON_CHANGE_ACTIONS.forEach(function (fn) {
            if (typeof(fn) === 'function') {
                fn();
            }
        });
    }

    function addOnChangeAction(fn) {
        if (typeof(fn) === 'function') {
            _ON_CHANGE_ACTIONS.push(fn);
        }
    }

    function disableOrEnableBillingAddress() {
        var $elems = $('input, select', $BILLING_ADDRESS[0]);
        if ($BILLING_ADDRESS.hasClass('is-hidden')) {
            $elems.attr('disabled', 'disabled');
        } else {
            $elems.removeAttr('disabled');
        }
    }

    function isEnabled() {
        return (BILLING_ADDRESS_AS_DELIVERY_ADDRESS_PICKER && BILLING_ADDRESS_AS_DELIVERY_ADDRESS_PICKER.checked)
    }

    return {
        init: function() {
            if (!BILLING_ADDRESS_AS_DELIVERY_ADDRESS_PICKER) {
                return;
            }
            disableOrEnableBillingAddress();
            bean.on(BILLING_ADDRESS_AS_DELIVERY_ADDRESS_PICKER, 'change', function () {
                $BILLING_ADDRESS.toggleClass('is-hidden');
                disableOrEnableBillingAddress();
                fireAllOnChangeActions();
            });
        },
        registerOnChangeAction: addOnChangeAction,
        isEnabled : isEnabled
    };
});
