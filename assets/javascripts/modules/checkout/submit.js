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

        guardian.experience = 'stripeCheckout';

        var handler;
        if (!('StripeCheckout' in window)) {
            raven.captureMessage('StripeCheckout not present');
            loader.stopLoader();
            submitEl.removeAttribute('disabled');
        }

        handler = StripeCheckout.configure(guardian.stripeCheckout);
        bean.on(window, 'popstate', handler.close);

        var successfulCharge = false;
        var ratePlan = document.querySelector('.js-rate-plans input:checked').dataset;
        var stripeServiceName = document.querySelectorAll('#personal-address-country option:checked')[0].dataset.stripeServiceName || 'UKStripeService';
        var stripePublicKeys = {'AUStripeService': guardian.stripe.auKey, 'UKStripeService': guardian.stripe.ukKey};

        handler.open({
            currency: ratePlan.currency,
            description: ratePlan.optionMirrorPackage,
            panelLabel: 'Subscribe',
            email: formElements.$EMAIL.val(),
            key: stripePublicKeys[stripeServiceName],
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
        var errorElement = $('.js-error');

        errorElement.text('');

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

                var paymentErr;
                var errorMessage;
                submitEl.removeAttribute('disabled');

                if(err.status === 403) {
                    //Only look for error JSON if the Checkout gives us an error.
                    try {
                        paymentErr = JSON.parse(err.response);
                        errorMessage = paymentErrorMessages.getMessage(paymentErr);
                    } catch (e) {
                        raven.Raven.captureException(e);
                    }
                }

                var userMessage = errorMessage || paymentErrorMessages.default

                if (errorElement) {
                    errorElement.text(userMessage);
                }
            }
        });
    }

    return {
        init: submitHandler
    };

});
