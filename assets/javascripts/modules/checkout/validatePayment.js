/* global Raven, Stripe */
define([
    'utils/ajax',
    'modules/forms/regex',
    'modules/checkout/validatePaymentFormat',
    'modules/checkout/stripeErrorMessages'
], function (ajax, regex, validatePaymentFormat, stripeErrorMessages) {
    'use strict';

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
        }).fail(function (err){
            Raven.captureException(err);
        });
    }

    function cardCheck(cardNumber, cardCVC, cardExpiryMonth, cardExpiryYear) {
        return new Promise(function (resolve) {
            Stripe.card.createToken({
                number: cardNumber,
                cvc: cardCVC,
                exp_month: cardExpiryMonth,
                exp_year: cardExpiryYear
            }, function(status, response) {
                console.log('STRIPE RESPONSE');
                console.log(response);

                var errMsg;
                if (response.error) {
                    // TODO: put this message on the page against credit card field
                    Raven.captureMessage(response.error.code + ' ' + response.error.message);
                    errMsg = stripeErrorMessages.getMessage(response.error);
                    if (errMsg) {
                        console.log('STRIPE ERROR: '+errMsg);
                    }
                    resolve(false);
                } else {
                    resolve(true);
                }
            });
        });
    }

    return function (data) {
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
                    console.log('CREDIT CARD');
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
    };

});
