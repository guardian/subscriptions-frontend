define(['modules/checkout/formElements',
        'modules/checkout/fieldSwitcher', '$', 'bean'], function (formEls,fieldSwitcher, $, bean) {
    'use strict';

    var billingAsDeliveryCheckbox = formEls.$BILLING_ADDRESS_AS_DELIVERY_ADDRESS_PICKER[0];
    var $BILLING_ADDRESS = formEls.BILLING.$CONTAINER;

    function disableOrEnableBillingAddress() {
        var $elems = $('input, select', $BILLING_ADDRESS[0]);
        if ($BILLING_ADDRESS.hasClass('is-hidden')) {
            $elems.attr('disabled', 'disabled');
        } else {
            $elems.removeAttr('disabled');
        }
        fieldSwitcher.update();
    }

    return {
        init: function() {
            if (!billingAsDeliveryCheckbox) {
                return;
            }
            disableOrEnableBillingAddress();
            bean.on(billingAsDeliveryCheckbox, 'change', function () {
                $BILLING_ADDRESS.toggleClass('is-hidden');
                disableOrEnableBillingAddress();
            });
        }
    };
});
