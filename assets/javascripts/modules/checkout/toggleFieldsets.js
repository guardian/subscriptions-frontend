define([
    'bean',
    'utils/ajax',
    'modules/checkout/formElements',
    'modules/checkout/validations'
], function (
    bean,
    ajax,
    formEls,
    validations
) {
    'use strict';

    var FIELDSET_COLLAPSED = 'fieldset--collapsed';
    var FIELDSET_COMPLETE = 'data-fieldset-complete';
    var IS_HIDDEN = 'is-hidden';

    function swap(from, to) {
        from.addClass(IS_HIDDEN);
        to.removeClass(IS_HIDDEN);
    }

    function collapseFieldsets(extra) {
        formEls.$FIELDSET_YOUR_DETAILS.addClass(FIELDSET_COLLAPSED);
        formEls.$FIELDSET_PAYMENT_DETAILS.addClass(FIELDSET_COLLAPSED);
        formEls.$FIELDSET_REVIEW.addClass(FIELDSET_COLLAPSED);
        if(extra) {
            extra.removeClass(FIELDSET_COLLAPSED);
        }
    }

    function submitHelper($actionEl, callbackFn) {
        if($actionEl.length) {
            bean.on($actionEl[0], 'click', function (evt) {
                evt.preventDefault();
                callbackFn();
            });
        }
    }

    function toggleFieldsetsDetails() {
        submitHelper(formEls.$YOUR_DETAILS_SUBMIT, function(){
            validations.validatePersonalDetails().then(function (validity) {
                if(validity.allValid) {
                    formEls.$FIELDSET_YOUR_DETAILS.addClass(FIELDSET_COLLAPSED).attr(FIELDSET_COMPLETE, '');
                    formEls.$FIELDSET_PAYMENT_DETAILS.removeClass(FIELDSET_COLLAPSED);
                    swap(formEls.$EDIT_YOUR_DETAILS, formEls.$EDIT_PAYMENT_DETAILS);
                }
            });
        });
    }

    function toggleFieldsetsPayment() {
        submitHelper(formEls.$PAYMENT_DETAILS_SUBMIT, function() {
            if(validations.validatePaymentDetails()){
                formEls.$FIELDSET_PAYMENT_DETAILS.addClass(FIELDSET_COLLAPSED).attr(FIELDSET_COMPLETE, '');
                formEls.$FIELDSET_REVIEW.removeClass(FIELDSET_COLLAPSED);
                formEls.$EDIT_PAYMENT_DETAILS.removeClass(IS_HIDDEN);
            }
        });
    }

    function toggleFieldsetsEdit() {
        var $editDetails = formEls.$EDIT_YOUR_DETAILS;
        var $editPayment = formEls.$EDIT_PAYMENT_DETAILS;

        if($editDetails.length && $editPayment.length){
            bean.on($editDetails[0], 'click', function (e) {
                e.preventDefault();
                collapseFieldsets(formEls.$FIELDSET_YOUR_DETAILS);
                $editDetails.addClass(IS_HIDDEN);
                if (formEls.$FIELDSET_PAYMENT_DETAILS.attr(FIELDSET_COMPLETE) !== null) {
                    $editPayment.removeClass(IS_HIDDEN);
                } else {
                    $editPayment.addClass(IS_HIDDEN);
                }
            });

            bean.on($editPayment[0], 'click', function (e) {
                e.preventDefault();
                collapseFieldsets(formEls.$FIELDSET_PAYMENT_DETAILS);
                swap($editPayment, $editDetails);
            });
        }
    }

    function init() {
        toggleFieldsetsDetails();
        toggleFieldsetsPayment();
        toggleFieldsetsEdit();
    }

    return {
        init: init
    };

});
