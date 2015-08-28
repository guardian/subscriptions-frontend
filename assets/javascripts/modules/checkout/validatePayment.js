define([
    'utils/ajax',
    'modules/forms/regex',
    'modules/checkout/validatePaymentFormat'
], function (ajax, regex, validatePaymentFormat) {
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

    return function (data) {
        var validity = validatePaymentFormat(data);

        return new Promise(function (resolve){
            if (validity.allValid) {
                accountCheck(data.accountNumber, data.sortCode, data.accountHolderName).then(function (accountValid) {
                    validity.accountNumberValid = accountValid;
                    validity.allValid = accountValid;
                    resolve(validity);
                });
            } else {
                validity.allValid = false;
                resolve(validity);
            }
        });
    };

});
