define([
    'modules/forms/toggleError',
    'modules/checkout/formElements',
    '$',
    'bean'
], function (
    toggleError,
    formEls,
    $,
    bean) {
    'use strict';

    var BILLING_ADDRESS_AS_DELIVERY_ADDRESS_PICKER = $('.js-checkout-delivery-sames-as-billing')[0];
    var $DELIVERY_ADDRESS = $('.js-checkout-delivery-address-container');

    var FIELDSET_COMPLETE = 'is-complete';
    var FIELDSET_COLLAPSED = 'is-collapsed';

    function disableOrEnableDeliveryAddress() {
        var $elems = $('input, select', $DELIVERY_ADDRESS[0]);

        if ($DELIVERY_ADDRESS.hasClass('is-hidden')) {
            $elems.attr('disabled', 'disabled');
        } else {
            $elems.removeAttr('disabled');
        }
    }

    function checkRequiredFields() {
        return $('input[required]:not([disabled]), select[required]:not([disabled])', $DELIVERY_ADDRESS[0]).map(function(f) {
            return $(f);
        }).map(function($field) {
            toggleError($field.parent(), !$field.val());
            return $field.val();
        }).reduce(function(f1, f2) {
            return f1 && f2;
        }, true);
    }

    return {
        init: function() {

            if (!$DELIVERY_ADDRESS.length) {
                return;
            }

            disableOrEnableDeliveryAddress();
            bean.on(BILLING_ADDRESS_AS_DELIVERY_ADDRESS_PICKER, 'change', function () {
                $DELIVERY_ADDRESS.toggleClass('is-hidden');
                disableOrEnableDeliveryAddress();
            });

            bean.on(formEls.$DELIVERY_DETAILS_SUBMIT[0], 'click', function(e) {

                e.preventDefault();
                if (!checkRequiredFields()) {
                    return;
                }

                formEls.$FIELDSET_PAYMENT_DETAILS.removeClass(FIELDSET_COLLAPSED);

                formEls.$FIELDSET_DELIVERY_DETAILS
                    .addClass(FIELDSET_COLLAPSED)
                    .addClass(FIELDSET_COMPLETE)[0]
                    .scrollIntoView();

                formEls.$NOTICES.removeAttr('hidden');
            });
        }
    };
});
