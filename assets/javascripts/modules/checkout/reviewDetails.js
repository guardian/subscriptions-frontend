define([
    'bean',
    'utils/ajax',
    'utils/text',
    'utils/serializer',
    'modules/forms/loader',
    'modules/forms/toggleError',
    'modules/checkout/formElements',
    'modules/checkout/payment'
], function (
    bean,
    ajax,
    textUtils,
    serializer,
    loader,
    toggleError,
    formEls,
    payment
) {
    'use strict';

    function clickHelper($elem, callback) {
        if ($elem.length) {
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

            formEls.$REVIEW_EMAIL.text(formEls.$EMAIL.val());
            formEls.$REVIEW_ACCOUNT.text(formEls.$ACCOUNT.val());
            formEls.$REVIEW_SORTCODE.text(formEls.$SORTCODE.val());
            formEls.$REVIEW_HOLDER.text(formEls.$HOLDER.val());

            var obscuredCardNumber = textUtils.obscure(formEls.$CARD_NUMBER.val(), 4, '*');
            formEls.$REVIEW_CARD_NUMBER.text(obscuredCardNumber);
            formEls.$REVIEW_CARD_EXPIRY.text(textUtils.mergeValues([
                formEls.$CARD_EXPIRY_MONTH.val(),
                formEls.$CARD_EXPIRY_YEAR.val()
            ], '/'));
        });
    }

    function submitHandler() {
        var submitEl;
        if (formEls.$CHECKOUT_SUBMIT.length) {
            submitEl = formEls.$CHECKOUT_SUBMIT[0];

            var form = formEls.$CHECKOUT_FORM[0];
            form.addEventListener('submit', function(e) {
                e.preventDefault();

                loader.startLoader();
                submitEl.setAttribute('disabled', 'disabled');

                var data = serializer([].slice.call(form.elements));

                if (data['payment.type'] === 'card') {
                    data['payment.token'] = payment.getStripeToken();

                    // TODO: do we want to remove direct debit data in this case?
                }

                ajax({
                    url: form.action,
                    method: 'post',
                    data: data,
                    success: function(successData) {
                        window.location.assign(successData.redirect);
                    },
                    error: function() {
                        loader.stopLoader();
                        submitEl.removeAttribute('disabled');

                        toggleError(formEls.$CARD_CONTAINER, true);
                    }
                })
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
