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
    'modules/checkout/formElements',
    'modules/analytics/ophan',
    'utils/cookie'
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
             formElements,
             ophan,
             cookie) {
    'use strict';

    function submitHandler() {
        if (formEls.$CHECKOUT_SUBMIT.length) {
            formEls.$CHECKOUT_SUBMIT[0].addEventListener('click', function(e) {
                ophan.loaded.then(function(o) { submit(e, o.viewId) });
            }, false);
        }
    }

    function submit(e, ophanPageViewId) {
        e.preventDefault();
        loader.setLoaderElem(document.querySelector('.js-loader'));
        loader.startLoader();
        var submitEl = formEls.$CHECKOUT_SUBMIT[0];

        submitEl.setAttribute('disabled', 'disabled');

        if (!formEls.$CARD_TYPE[0].checked) {
            send(ophanPageViewId);
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
        handler.open({
            currency: ratePlan.currency,
            description: ratePlan.optionMirrorPackage,
            panelLabel: 'Subscribe',
            email: formElements.$EMAIL.val(),
            token: function (token) {
                successfulCharge = token;
                stripeCheckout.setPaymentToken(token.id);
                send(ophanPageViewId);
            },
            closed: function () {
                if (!successfulCharge) {
                    loader.stopLoader();
                    submitEl.removeAttribute('disabled');
                }
            }
        });
    }

    function send(ophanPageViewId) {
        var form = formEls.$CHECKOUT_FORM[0];
        var data = serializer([].slice.call(form.elements));
        var submitEl = formEls.$CHECKOUT_SUBMIT[0];
        var errorElement = $('.js-error');

        errorElement.text('');

        data.ophanBrowserId = cookie.getCookie('bwid');
        data.ophanVisitId = cookie.getCookie('vsid');
        data.ophanPageViewId = ophanPageViewId;

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
