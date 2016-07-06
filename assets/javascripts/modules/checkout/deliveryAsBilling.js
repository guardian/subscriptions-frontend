define(['modules/checkout/formElements', '$', 'bean'], function (formEls, $, bean) {
    'use strict';

    var BILLING_ADDRESS_AS_DELIVERY_ADDRESS_PICKER = $('.js-checkout-delivery-sames-as-billing')[0];
    var $BILLING_ADDRESS = formEls.BILLING.$CONTAINER;
    
    function disableOrEnableBillingAddress() {
        var $elems = $('input, select', $BILLING_ADDRESS[0]);
        if ($BILLING_ADDRESS.hasClass('is-hidden')) {
            $elems.attr('disabled', 'disabled');
        } else {
            $elems.removeAttr('disabled');
        }
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
