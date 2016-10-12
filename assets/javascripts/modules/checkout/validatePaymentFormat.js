/* global Stripe */
define(['modules/forms/regex'], function (regex) {
    'use strict';

    return function (data) {
        var validity = {
            accountNumberValid: true,
            accountHolderNameValid: true,
            sortCodeValid: true,
            detailsConfirmedValid: true,

            cardNumberValid: true,
            cardCVCValid: true,
            cardExpiryValid: true
        };

        if (data.paymentMethod === 'direct-debit') {
            validity.accountNumberValid = (
                data.accountNumber !== '' &&
                data.accountNumber.length >= 6 &&
                data.accountNumber.length <= 10 &&
                regex.isNumber(data.accountNumber)
            );

            validity.accountHolderNameValid = (
                data.accountHolderName !== '' &&
                data.accountHolderName.length <= 18
            );

            validity.sortCodeValid = data.sortCode && (data.sortCode.split('-')).filter(function (code) {
                    var codeAsNumber = parseInt(code, 10);
                    return codeAsNumber >= 0 && codeAsNumber <= 99;
                }).length === 3;

            validity.detailsConfirmedValid = data.detailsConfirmed;

            validity.allValid = (
                validity.accountNumberValid &&
                validity.accountHolderNameValid &&
                validity.sortCodeValid &&
                validity.detailsConfirmedValid
            );
            return validity;

        }

        if (guardian.stripeCheckout && data.paymentMethod === 'card') {
            validity.allValid = true;
            return validity;

        }
        if (data.paymentMethod === 'card') {
            validity.cardNumberValid = Stripe.card.validateCardNumber(data.cardNumber);
            validity.cardCVCValid = Stripe.card.validateCVC(data.cardCVC);
            validity.cardExpiryValid = Stripe.card.validateExpiry(data.cardExpiryMonth, data.cardExpiryYear);

            validity.allValid = (
                validity.cardNumberValid &&
                validity.cardCVCValid &&
                validity.cardExpiryValid
            );
            return validity;
        }


    };

});
