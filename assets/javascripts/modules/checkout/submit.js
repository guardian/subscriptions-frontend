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
                e.preventDefault();
                ophan.loaded.then(submit, submit)
            }, false);
        }
    }

    function submit(ophan) {
        loader.setLoaderElem(document.querySelector('.js-loader'));
        loader.startLoader();
        var submitEl = formEls.$CHECKOUT_SUBMIT[0];
        var pageViewId = ophan && ophan.viewId;

        submitEl.setAttribute('disabled', 'disabled');

        if (!formEls.$CARD_TYPE[0].checked) {
            send(pageViewId);
            return;
        }

        guardian.experience = 'stripeCheckout';

        var handler;
        if (!('StripeCheckout' in window)) {
            raven.captureMessage('StripeCheckout not present');
            loader.stopLoader();
            submitEl.removeAttribute('disabled');
        }

        var stripeKey = deriveStripeKeyFromDeliveryCountry()

        handler = StripeCheckout.configure(Object.assign({}, guardian.stripeCheckout, {'key' : stripeKey}));
        bean.on(window, 'popstate', handler.close);

        var successfulCharge = false;
        var ratePlan = document.querySelector('.js-rate-plans input:checked').dataset;

        handler.open({
            currency: ratePlan.currency,
            description: ratePlan.optionMirrorPackage,
            panelLabel: 'Subscribe',
            email: formElements.$EMAIL.val(),
            key: stripeKey,
            token: function (token) {
                successfulCharge = token;
                stripeCheckout.setPaymentToken(token.id);
                send(pageViewId);
            },
            closed: function () {
                if (!successfulCharge) {
                    loader.stopLoader();
                    submitEl.removeAttribute('disabled');
                }
            }
        });
    }

    function deriveStripeKeyFromDeliveryCountry() {
        var deliveryAddressOption = document.querySelectorAll('#delivery-address-country option:checked')[0];
        var stripeServiceName = (deliveryAddressOption && deliveryAddressOption.dataset.stripeServiceName) || 'ukPublicKey';
        return guardian.stripe[stripeServiceName]
    }

    function send(pageViewId) {
        var form = formEls.$CHECKOUT_FORM[0];
        var data = serializer([].slice.call(form.elements));
        var submitEl = formEls.$CHECKOUT_SUBMIT[0];
        var errorElement = $('.js-error');

        errorElement.text('');

        data['ophan.browserId'] = cookie.getCookie('bwid');
        data['ophan.visitId'] = cookie.getCookie('vsid');
        data['ophan.pageViewId'] = pageViewId;

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
