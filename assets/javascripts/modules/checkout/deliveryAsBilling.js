define(['modules/checkout/formElements', '$', 'bean', 'modules/checkout/fieldSwitcher'], function (formEls, $, bean, fieldSwitcher) {
    'use strict';

    var BILLING_ADDRESS_AS_DELIVERY_ADDRESS_PICKER = $('.js-checkout-delivery-sames-as-billing')[0];
    var $BILLING_ADDRESS = formEls.BILLING.$CONTAINER;

    function disableOrEnableBillingAddress() {
        var $elems = $('input, select', $BILLING_ADDRESS[0]);
        var deliveryAsBilling = false;
        if ($BILLING_ADDRESS.hasClass('is-hidden')) {
            $elems.attr('disabled', 'disabled');
            deliveryAsBilling = true;
        } else {
            $elems.removeAttr('disabled');
        }
        fieldSwitcher.setDeliveryAsBillingAddress(deliveryAsBilling);
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
            });
        }
    };
});
