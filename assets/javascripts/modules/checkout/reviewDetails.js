define([
    'bean',
    'utils/ajax',
    'utils/text',
    'modules/forms/loader',
    'modules/checkout/formElements'
], function (
    bean,
    ajax,
    textUtils,
    loader,
    formEls
) {
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

    function submitHandler() {
        var submitEl;
        if(formEls.$CHECKOUT_SUBMIT.length) {
            submitEl = formEls.$CHECKOUT_SUBMIT[0];

            var form = formEls.$CHECKOUT_FORM[0];
            form.addEventListener('submit', function() {
                loader.startLoader();
                submitEl.setAttribute('disabled', 'disabled');
            }, false);
        }

    }

    function init() {
        populateDetails();
        submitHandler();
    }

    return {
        init: init
    };

});
