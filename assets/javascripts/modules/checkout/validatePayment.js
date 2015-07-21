define(['modules/forms/regex'], function (regex) {
    'use strict';

    return function (data) {

        var validity = {};

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

        validity.sortCodeValid = data.sortCodeParts.filter(function (code) {
            var codeAsNumber = parseInt(code, 10);
            return (
                code.length === 2 &&
                codeAsNumber >= 0 && codeAsNumber <= 99
            );
        }).length === 3;

        validity.detailsConfirmedValid = data.detailsConfirmed;

        validity.allValid = (
            validity.accountNumberValid &&
            validity.accountHolderNameValid &&
            validity.sortCodeValid &&
            validity.detailsConfirmedValid
        );

        return validity;
    };

});
