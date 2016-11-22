/* global guardian */
define([
    'utils/ajax',
    'modules/checkout/validatePaymentFormat',
    'modules/checkout/stripeCheckout',
    'modules/raven'
], function (ajax, validatePaymentFormat, stripeCheckout, raven) {
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
        }).fail(function (err) {
            raven.Raven.captureException(err);
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
                    validity.allValid = true;
                    resolve(validity);
                    return;
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
