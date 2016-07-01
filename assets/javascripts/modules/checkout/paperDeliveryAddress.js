define([
    'modules/checkout/formElements',
    '$',
    'bean'
], function (
    formEls,
    $,
    bean) {
    'use strict';

    var BILLING_ADDRESS_AS_DELIVERY_ADDRESS_PICKER = $('.js-checkout-delivery-sames-as-billing')[0];
    var $DELIVERY_ADDRESS = $('.js-checkout-delivery-address-container');

    var FIELDSET_COMPLETE = 'is-complete';
    var FIELDSET_COLLAPSED = 'is-collapsed';

    return {
        init: function() {

            if (!$DELIVERY_ADDRESS.length) {
                return;
            }

            bean.on(BILLING_ADDRESS_AS_DELIVERY_ADDRESS_PICKER, 'change', function () {
                $DELIVERY_ADDRESS.toggleClass('is-hidden');
            });

            bean.on(formEls.$DELIVERY_DETAILS_SUBMIT[0], 'click', function(e) {
                e.preventDefault();
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
