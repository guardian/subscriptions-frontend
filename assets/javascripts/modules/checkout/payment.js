/* global Stripe */
define([
    'utils/ajax',
    'modules/forms/regex',
    'modules/checkout/formElements',
    'modules/checkout/validatePaymentFormat',
    'modules/checkout/stripeCheckout',
    'modules/raven'
], function (ajax, regex, formElements, validatePaymentFormat, stripeCheckout, raven) {
    'use strict';
    //TODO: STRIPE CHECKOUT
    var ACCOUNT_CHECK_ENDPOINT = '/checkout/check-account';

    function accountCheck(accountNumber, sortCode, accountHolderName) {
        return ajax({
            url: ACCOUNT_CHECK_ENDPOINT,
            method: 'post',
            type: 'json',
            data: {
                account: accountNumber,
                sortcode: sortCode,
                holder: accountHolderName
            }
        }).then(function (response) {
            return response.accountValid;
        }).fail(function (err) {
            raven.Raven.captureException(err);
        });
    }

    function cardCheck(cardNumber, cardCVC, cardExpiryMonth, cardExpiryYear) {
        return new Promise(function (resolve) {
            Stripe.card.createToken({
                number: cardNumber,
                cvc: cardCVC,
                exp_month: cardExpiryMonth,
                exp_year: cardExpiryYear
            }, function (status, response) {
                if (response.error) {
                    // Since we've already validated the card details clientside using Stripe's
                    // library, an error during token creation is unexpected. For simplicity we
                    // just log the error and display a message against the card number in such cases.
                    raven.Raven.captureMessage(response.error.code + ' ' + response.error.message);
                    resolve(false);
                } else {
                    stripeCheckout.setPaymentToken(response.id);
                    resolve(true);
                }
            });
        });
    }


    function validate(data) {
        var validity = validatePaymentFormat(data);
        return new Promise(function (resolve) {
            if (validity.allValid) {
                if (data.paymentMethod === 'direct-debit') {
                    accountCheck(data.accountNumber, data.sortCode, data.accountHolderName).then(function (accountValid) {
                        validity.accountNumberValid = accountValid;
                        validity.allValid = accountValid;
                        resolve(validity);

                    });
                } else {

                    if (guardian.stripeCheckout) {
                        validity.allValid = true;// valid;
                        resolve(validity);
                        return;
                    }
                    cardCheck(data.cardNumber, data.cardCVC, data.cardExpiryMonth, data.cardExpiryYear).then(function (cardValid) {
                        validity.cardNumberValid = cardValid;
                        validity.allValid = cardValid;
                        resolve(validity);
                    });

                }
            } else {
                validity.allValid = false;
                resolve(validity);
            }
        });
    }


    return {
        validate: validate
    };

});
