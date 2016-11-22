/* global StripeCheckout */
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
    'modules/raven',
    'modules/checkout/formElements'
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
             raven,
             formElements) {
    'use strict';

    var FIELDSET_COLLAPSED = 'is-collapsed';

    function submitHandler() {
        if (formEls.$CHECKOUT_SUBMIT.length) {

            formEls.$CHECKOUT_SUBMIT[0].addEventListener('click', submit, false);
        }

    }

    function submit(e) {
        e.preventDefault();
        loader.setLoaderElem(document.querySelector('.js-loader'));
        loader.startLoader();
        var submitEl = formEls.$CHECKOUT_SUBMIT[0];

        submitEl.setAttribute('disabled', 'disabled');

        if (!formEls.$CARD_TYPE[0].checked) {
            send();
            return;
        }

        var handler;
        if (!('StripeCheckout' in window)) {
            raven.captureMessage('StripeCheckout not present');
            loader.stopLoader();
            submitEl.removeAttribute('disabled');
        }

        handler = StripeCheckout.configure(guardian.stripeCheckout);
        bean.on(window, 'popstate', function () {
            handler.close()
        });

        var successfulCharge = false;
        var ratePlan = document.querySelector('.js-rate-plans input:checked').dataset;
        handler.open({
            allowRememberMe: false,
            currency: ratePlan.currency,
            description: ratePlan.optionMirrorPackage,
            panelLabel: 'Subscribe',
            email: formElements.$EMAIL.val(),
            token: function (token) {
                successfulCharge = token;
                stripeCheckout.setPaymentToken(token.id);
                send();
            },
            closed: function () {
                if (!successfulCharge) {
                    loader.stopLoader();
                    submitEl.removeAttribute('disabled');
                }
            }
        });


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
