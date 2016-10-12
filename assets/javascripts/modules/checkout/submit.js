define([
    '$',
    'bean',
    'utils/ajax',
    'utils/text',
    'utils/serializer',
    'modules/forms/loader',
    'modules/forms/toggleError',
    'modules/checkout/formElements',
    'modules/checkout/eventTracking',
    'modules/checkout/stripeErrorMessages',
    'modules/checkout/stripeCheckout',
    'modules/raven'
], function ($,
             bean,
             ajax,
             textUtils,
             serializer,
             loader,
             toggleError,
             formEls,
             eventTracking,
             paymentErrorMessages,
             stripeCheckout,
             raven) {
    'use strict';

    var FIELDSET_COLLAPSED = 'is-collapsed';

    function useCheckout() {
        return guardian.stripeCheckout && formEls.$CARD_TYPE[0].checked
    }



    function submitHandler() {
        if (formEls.$CHECKOUT_SUBMIT.length) {

            formEls.$CHECKOUT_FORM[0].addEventListener('submit', submit, false);
        }

    }

    function submit(e) {
        e.preventDefault();
        loader.setLoaderElem(document.querySelector('.js-loader'));
        loader.startLoader();
        var submitEl = formEls.$CHECKOUT_SUBMIT[0];

        submitEl.setAttribute('disabled', 'disabled');

        if (!useCheckout()) {
            send();
            return;
        }

        stripeCheckout.openCheckout().then(send, function () {
                loader.stopLoader();
                submitEl.removeAttribute('disabled');
            }
        );
    }

    function send() {
        var form = formEls.$CHECKOUT_FORM[0];
        var data = serializer([].slice.call(form.elements));
        var submitEl = formEls.$CHECKOUT_SUBMIT[0];
        ajax({
            url: '/checkout',
            method: 'post',
            data: data,
            success: function (successData) {
                eventTracking.completedReviewDetails();
                window.location.assign(successData.redirect);
            },
            error: function (err) {
                loader.stopLoader();
                submitEl.removeAttribute('disabled');

                formEls.$FIELDSET_PAYMENT_DETAILS
                    .removeClass(FIELDSET_COLLAPSED);

                formEls.$FIELDSET_REVIEW
                    .addClass(FIELDSET_COLLAPSED);

                formEls.$FIELDSET_YOUR_DETAILS[0]
                    .scrollIntoView();

                toggleError(formEls.$CARD_CONTAINER, true);

                var paymentErr;

                try {
                    paymentErr = JSON.parse(err.response);
                } catch (e) {
                    raven.Raven.captureException(e);
                }

                var userMessage = paymentErrorMessages.getMessage(paymentErr);
                var errorElement = paymentErrorMessages.getElement(paymentErr);
                if (errorElement) {
                    errorElement.textContent = userMessage;
                }
            }
        })
    }

    function init() {
        submitHandler();
    }

    return {
        init: init
    };

});
