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

    function collapseFieldsetsExcept(leaveOpen) {
        [
            formEls.$FIELDSET_YOUR_DETAILS,
            formEls.$FIELDSET_PAYMENT_DETAILS,
            formEls.$FIELDSET_REVIEW
        ].forEach(function(item) {
            if (item === leaveOpen) {
                item.removeClass(FIELDSET_COLLAPSED);
            } else {
                item.addClass(FIELDSET_COLLAPSED);
            }
        });
    }

    function init() {
        var $editDetails = formEls.$EDIT_YOUR_DETAILS;
        var $editPayment = formEls.$EDIT_PAYMENT_DETAILS;

        if ($editDetails.length && $editPayment.length) {
            bean.on($editDetails[0], 'click', function(e) {
                e.preventDefault();
                collapseFieldsetsExcept(formEls.$FIELDSET_YOUR_DETAILS);
                formEls.$FIELDSET_YOUR_DETAILS.removeClass(FIELDSET_COMPLETE);
                formEls.$FIELDSET_PAYMENT_DETAILS.removeClass(FIELDSET_COMPLETE);
                formEls.$NOTICES.attr('hidden', true);
                tracking.personalDetailsTracking();
            });

            bean.on($editPayment[0], 'click', function(e) {
                e.preventDefault();
                collapseFieldsetsExcept(formEls.$FIELDSET_PAYMENT_DETAILS);
                formEls.$FIELDSET_PAYMENT_DETAILS.removeClass(FIELDSET_COMPLETE);
                tracking.paymentDetailsTracking();
            });
        }
    }

    return {
        init: init
    };

});
