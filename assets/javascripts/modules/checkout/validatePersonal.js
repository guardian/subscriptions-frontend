define([
    'utils/ajax',
    'modules/forms/regex'
], function (ajax, regex) {
    'use strict';

    function emailCheck(email) {
        return ajax({ url: '/checkout/check-identity?email=' + email }).then(function (response) {
            return response.emailInUse;
        }).fail(function (err) {
            Raven.captureException(err);
        });
    }

    return function(data) {

        var MESSAGES = {
            emailInvalid: 'Please enter a valid email address.',
            emailTaken: 'Your email is already in use. Please sign in or use another email address.',
            emailFailure: 'There has been a problem. Please try again later.'
        };

        return new Promise(function (resolve) {

            var emailValue = data.emailAddress;
            var hasValidEmail = regex.isValidEmail(emailValue);
            var confirmationEmailValue = data.emailAddressConfirmed;
            var hasConfirmedEmail = emailValue === confirmationEmailValue;

            var emptyFields = data.requiredFields.filter(function (field) {
                return !field.input.value;
            });

            var fieldsTooLong =
                data.lengthCheckedFields.filter(function (field) {
                    return field.input.value.length > field.maxLength;
                });

            var hasBasicValidity = (
                hasValidEmail &&
                hasConfirmedEmail &&
                !emptyFields.length &&
                !fieldsTooLong.length
            );

            var validity = {
                allValid: false,
                emptyFields: emptyFields,
                requiredFields: data.requiredFields,
                hasValidEmail: hasValidEmail,
                hasConfirmedEmail: hasConfirmedEmail,
                emailMessage: (hasValidEmail) ? false : MESSAGES.emailInvalid,
                isEmailInUse: false,
                fieldsTooLong: fieldsTooLong
            };

            if(hasBasicValidity) {
                if( guardian.user.isSignedIn ) {
                    validity.allValid = true;
                    resolve(validity);
                } else {
                    emailCheck(emailValue).then(function(isEmailInUse) {
                        if(isEmailInUse) {
                            validity.isEmailInUse = true;
                            validity.emailMessage = MESSAGES.emailTaken;
                            resolve(validity);
                        } else {
                            validity.allValid = true;
                            validity.emailMessage = false;
                            resolve(validity);
                        }
                    }).fail(function(err) {
                        Raven.captureException(err);
                        validity.emailMessage = MESSAGES.emailFailure;
                        resolve(validity);
                    });
                }
            } else {
                resolve(validity);
            }
        });
    };

});
