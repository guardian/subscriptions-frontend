/* global guardian, StripeCheckout */

define([
    'bean',
    'modules/checkout/formElements',
    'raven-js'
], function (bean, formElements, raven) {
    function openCheckout() {
        return new Promise(function (resolve, reject) {
            var handler;
            if (!('StripeCheckout' in window)) {
                raven.captureMessage('StripeCheckout not present');
                resolve(false);
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
                    setPaymentToken(token.id);
                    resolve(token);
                },
                closed: function () {
                    if (!successfulCharge) {
                        reject();
                    }
                }
            })
        });

    }

    function setPaymentToken(token) {
        var field = document.querySelector('.js-payment-token');
        field.value = token
    }

    return ({
        setPaymentToken: setPaymentToken,
        openCheckout: openCheckout
    })
});
