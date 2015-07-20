define([
    'bean',
    'utils/text',
    'modules/checkout/formElements'
], function (bean, textUtils, formEls) {
    'use strict';

    function populateDetails() {
        formEls.$REVIEW_NAME.text(textUtils.mergeValues([
            formEls.$FIRST_NAME.val(),
            formEls.$LAST_NAME.val()
        ], ' '));

        formEls.$REVIEW_ADDRESS.text(textUtils.mergeValues([
            formEls.$ADDRESS1.val(),
            formEls.$ADDRESS2.val(),
            formEls.$ADDRESS3.val(),
            formEls.$POSTCODE.val()
        ], ', '));

        formEls.$REVIEW_EMAIL.text(textUtils.mergeValues([
            formEls.$EMAIL.val()
        ], ''));

        formEls.$REVIEW_ACCOUNT.text(textUtils.mergeValues([
            formEls.$ACCOUNT.val()
        ], ''));

        formEls.$REVIEW_SORTCODE.text(textUtils.mergeValues([
            formEls.$SORTCODE1.val(),
            formEls.$SORTCODE2.val(),
            formEls.$SORTCODE3.val()
        ], '-'));

        formEls.$REVIEW_HOLDER.text(textUtils.mergeValues([
            formEls.$HOLDER.val()
        ], ''));
    }

    function init() {
        var $detailsSubmit = formEls.$PAYMENT_DETAILS_SUBMIT;
        if($detailsSubmit.length) {
            bean.on($detailsSubmit[0], 'click', function (evt) {
                evt.preventDefault();
                populateDetails();
            });
        }
    }

    return {
        init: init
    };

});
