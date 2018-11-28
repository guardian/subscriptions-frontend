define(['modules/checkout/formElements','$', 'bean'], function (formEls, $, bean) {
    'use strict';

    const $GIFT_RECIPIENT_DETAILS = $('.js-gift-recipient-details');
    const $IS_A_GIFT_CHECKBOX = $('.js-checkout-is-a-gift .js-input');
    const $DELIVERY_DETAILS_LEGEND = $('.js-delivery-details-legend');
    const $BILL_MY_DELIVERY_ADDRESS_SECTION = $('.js-checkout-use-delivery');
    const $CHECKOUT_DELIVERY_SAMES_AS_BILLING = $('.js-checkout-delivery-same-as-billing');
    const $BILLING_ADDRESS_SECTION = $('.js-billing-address');

    function disableOrEnableAllFields($parentNode) {
        if ($parentNode.length === 1) {
            const $elems = $('.js-input', $parentNode[0]);
            if ($parentNode.hasClass('is-hidden')) {
                $elems.attr('disabled', 'disabled');
                $elems.removeAttr('checked');
                $elems.val('');
            } else {
                $elems.removeAttr('disabled');
            }
        }
    }

    return {
        init: function() {
            if ($IS_A_GIFT_CHECKBOX.length === 1) {
                bean.on($IS_A_GIFT_CHECKBOX[0], 'change', function (event) {
                    if (event.target.checked) {
                        $CHECKOUT_DELIVERY_SAMES_AS_BILLING.removeAttr('checked');
                        $CHECKOUT_DELIVERY_SAMES_AS_BILLING.attr('disabled', 'disabled');
                        $DELIVERY_DETAILS_LEGEND.text('Gift recipient details');
                        $GIFT_RECIPIENT_DETAILS.removeClass('is-hidden');
                        $BILL_MY_DELIVERY_ADDRESS_SECTION.addClass('is-hidden');
                        $BILLING_ADDRESS_SECTION.removeClass('is-hidden');
                    } else {
                        $CHECKOUT_DELIVERY_SAMES_AS_BILLING.attr('checked');
                        $DELIVERY_DETAILS_LEGEND.text('Delivery address');
                        $GIFT_RECIPIENT_DETAILS.addClass('is-hidden');
                        //$BILL_MY_DELIVERY_ADDRESS_SECTION.removeClass('is-hidden');
                        //$BILLING_ADDRESS_SECTION.addClass('is-hidden');
                    }
                    disableOrEnableAllFields($GIFT_RECIPIENT_DETAILS);
                    disableOrEnableAllFields($BILLING_ADDRESS_SECTION);
                });
            }
        }
    };
});
