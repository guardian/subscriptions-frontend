define([
    'bean',
    'modules/checkout/formElements',
    'modules/checkout/tracking'
], function (
    bean,
    formEls,
    tracking) {
    'use strict';

    var FIELDSET_COMPLETE = 'is-complete';
    var FIELDSET_COLLAPSED = 'is-collapsed';

    function collapseFieldsets(extra) {
        [
            formEls.$FIELDSET_YOUR_DETAILS,
            formEls.$FIELDSET_PAYMENT_DETAILS,
            formEls.$FIELDSET_REVIEW
        ].forEach(function(item) {
            item.addClass(FIELDSET_COLLAPSED);
        });
        if(extra) {
            extra.removeClass(FIELDSET_COLLAPSED);
        }
    }

    function init() {
        var $editDetails = formEls.$EDIT_YOUR_DETAILS;
        var $editPayment = formEls.$EDIT_PAYMENT_DETAILS;

        if($editDetails.length && $editPayment.length){

            bean.on($editDetails[0], 'click', function (e) {
                e.preventDefault();
                collapseFieldsets(formEls.$FIELDSET_YOUR_DETAILS);
                formEls.$FIELDSET_YOUR_DETAILS.removeClass(FIELDSET_COMPLETE);
                formEls.$FIELDSET_PAYMENT_DETAILS.removeClass(FIELDSET_COMPLETE);
                tracking.personalDetailsTracking();
            });

            bean.on($editPayment[0], 'click', function (e) {
                e.preventDefault();
                collapseFieldsets(formEls.$FIELDSET_PAYMENT_DETAILS);
                formEls.$FIELDSET_PAYMENT_DETAILS.removeClass(FIELDSET_COMPLETE);
                tracking.paymentDetailsTracking();
            });

        }
    }

    return {
        init: init
    };

});
