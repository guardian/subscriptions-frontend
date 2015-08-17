define([
    'bean',
    'utils/ajax',
    'utils/text',
    'modules/checkout/formElements',
    'modules/checkout/tracking'
], function (
    bean,
    ajax,
    textUtils,
    formEls,
    tracking) {
    'use strict';

    function clickHelper($elem, callback) {
        if($elem.length) {
            bean.on($elem[0], 'click', function (e) {
                e.preventDefault();
                callback();
            });
        }
    }

    function populateDetails() {
        clickHelper(formEls.$PAYMENT_DETAILS_SUBMIT, function() {
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
                formEls.$SORTCODE.val()
            ], ''));

            formEls.$REVIEW_HOLDER.text(textUtils.mergeValues([
                formEls.$HOLDER.val()
            ], ''));
        });
    }

    function paymentSubmissionTracking() {
        if(formEls.$CHECKOUT_SUBMIT.length) {
            bean.on(formEls.$CHECKOUT_SUBMIT[0], 'click', function () {
                tracking.paymentSubmissionTracking();
            });
        }
    }

    function init() {
        populateDetails();
        paymentSubmissionTracking();
    }

    return {
        init: init
    };

});
