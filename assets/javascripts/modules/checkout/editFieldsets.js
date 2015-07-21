define(['bean', 'modules/checkout/formElements'], function (bean, formEls) {
    'use strict';

    var FIELDSET_COLLAPSED = 'fieldset--collapsed';
    var FIELDSET_COMPLETE = 'data-fieldset-complete';
    var HIDDEN_CLASS = 'is-hidden';

    function swap(from, to) {
        from.addClass(HIDDEN_CLASS);
        to.removeClass(HIDDEN_CLASS);
    }

    function collapseFieldsets(extra) {
        formEls.$FIELDSET_YOUR_DETAILS.addClass(FIELDSET_COLLAPSED);
        formEls.$FIELDSET_PAYMENT_DETAILS.addClass(FIELDSET_COLLAPSED);
        formEls.$FIELDSET_REVIEW.addClass(FIELDSET_COLLAPSED);
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

                $editDetails.addClass(HIDDEN_CLASS);

                if (formEls.$FIELDSET_PAYMENT_DETAILS.attr(FIELDSET_COMPLETE) !== null) {
                    $editPayment.removeClass(HIDDEN_CLASS);
                } else {
                    $editPayment.addClass(HIDDEN_CLASS);
                }
            });

            bean.on($editPayment[0], 'click', function (e) {
                e.preventDefault();

                collapseFieldsets(formEls.$FIELDSET_PAYMENT_DETAILS);
                swap($editPayment, $editDetails);
            });
        }
    }

    return {
        init: init
    };

});
