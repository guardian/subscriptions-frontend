/* global Stripe, StripeCheckout */
define([
    'utils/ajax',
    'modules/forms/regex',
    'modules/checkout/formElements',
    'modules/checkout/validatePaymentFormat',
    'modules/checkout/reviewDetails',
    'modules/raven',
    'bean'
], function (ajax, regex, formElements, validatePaymentFormat, reviewDetails, raven, bean) {
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
                    setPaymentToken(response.id);
                    resolve(true);
                }
            });
        });
    }

    function openCheckout() {
        return new Promise(function (resolve) {
            var handler;
            if (!('StripeCheckout' in window)) {
                console.error('handle this with raven');
                //TODO: raven
                resolve(false);
            }

            handler = StripeCheckout.configure({
                key: guardian.stripePublicKey,
                image: 'https://lh3.googleusercontent.com/jVhjR06ijlH-AsQUOtEMMC2ujBm8idfbWVNIgJukSO9sXL89ct1FcTtNd8IKdewWrMDLewcyfpkfGzy7JbqgMiwn1VTLL2i9KRt-g1vaugpbbKn2bgtregSoRRdViydPmXIkdBr3FLMU3rnbT4_d4NeBdQKwzu7FCm4xPGGlFjyKq2YNVfJKKFO5AyzHmcRrsgH2Tma6Q3LPpyHCwJPP0-HuafZq2K-LZjFvuE2DNdKQ7mk6J5qsy2ULHp37xANW9Krtx1OZe-Gqte1fqVnz9VdOZga6spF2RSbNPjnbk6AEjSSw9Yn-0hFvHvjJmWmoAkXftpkco3_Dr2TY65YCYUOP9AK-WCFkANChEZsohHWlDQhyRvjmelHnHk_-52ZCWeiLS-ixmgaa0PKdqwkMdKHqJ0ZXUuW8UyhJ1Usvu4MGQC49qz9JYFYKC3-TSYEhaf4LyUqfDc1B8P85GgXDp_3N-4VPBtRyBY9K30nP6hl9_FP4dn97ZIsIvv2J7opUOP_RnrwoKVxhdj5MRGvFbLzUmqooAnSZ0II-xNLfZd0Jzs1R9X8zzmsHJ-dokBDwujET8NzjJeqN0TexHMsEvSMHoU5INClY7G8q9TQ-lA2U-A9rwg=w1324-h1764-no',
                locale: 'auto',
                name: 'Guardian',
                zipCode: false
            });
            bean.on(window, 'popstate', function () {
                handler.close()
            });

            var successfulCharge = false;
            var ratePlan = document.querySelector('.js-rate-plans input:checked').dataset;
            handler.open({
                token: function (token) {
                    successfulCharge = true;
                    setPaymentToken(token.id);
                    reviewDetails.repopulateDetails(token);
                },
                closed: function(){
                    resolve(successfulCharge);
                },
                amount: ratePlan.amount * 100,
                currency: ratePlan.currency,
                description: ratePlan.optionMirrorPackage,
                panelLabel: ratePlan.stripeButton,
                email: formElements.$EMAIL.val()
            })
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
                        openCheckout().then(function (valid) {
                            validity.allValid = valid;
                            resolve(validity);
                        })
                    } else {
                        cardCheck(data.cardNumber, data.cardCVC, data.cardExpiryMonth, data.cardExpiryYear).then(function (cardValid) {
                            validity.cardNumberValid = cardValid;
                            validity.allValid = cardValid;
                            resolve(validity); 

                        });
                    }
                }
            } else {
                validity.allValid = false;
                resolve(validity);
            }
        });
    }

    function setPaymentToken(token){
        var field = document.querySelector('.js-payment-token');
        field.value = token
    }

    return {
        validate: validate
    };

});
