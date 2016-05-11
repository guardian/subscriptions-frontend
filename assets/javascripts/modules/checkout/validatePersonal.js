define([
    'utils/ajax',
    'modules/forms/regex',
    'raven'
], function (ajax, regex, raven) {
    'use strict';

    function emailCheck(email) {
        return ajax({ url: '/checkout/check-identity?email=' + encodeURIComponent(email) }).then(function (response) {
            return response.emailInUse;
        }).fail(function (err) {
            raven.Raven.captureException(err);
        });
    }

    return function(data, isSignedIn) {

        var MESSAGES = {
            emailInvalid: 'Please enter a valid email address.',
            emailTaken: 'Your email is already in use. Please sign in or use another email address.',
            emailFailure: 'There has been a problem. Please try again later.'
        };

        /**
         * Return validity object as a promise as parts of the
         * validation process can be asyncronous.
         */
        return new Promise(function (resolve) {

            var emailValue = data.emailAddress;
            var confirmedEmailValue = data.emailAddressConfirmed;

            var hasValidEmail = regex.isValidEmail(emailValue);
            var hasConfirmedEmail = emailValue === confirmedEmailValue;

            var emptyFields = data.requiredFieldValues.filter(function (field) {
                return !field;
            });

            var hasBasicValidity = !emptyFields.length;

            var hasBasicEmailValidity = (
                hasValidEmail &&
                hasConfirmedEmail
            );

            var validity = {
                allValid: false,
                emptyFields: emptyFields,
                requiredFieldValues: data.requiredFieldValues,
                hasValidEmail: hasValidEmail,
                hasConfirmedEmail: hasConfirmedEmail,
                emailMessage: (hasValidEmail) ? false : MESSAGES.emailInvalid,
                isEmailInUse: false
            };

            if (hasBasicValidity) {

                /**
                 * If the user is signed in we do not need to
                 * validate their email address
                 */
                if (isSignedIn) {
                    validity.allValid = true;
                    resolve(validity);
                }

                /**
                 * If the user is anonymous / signed-out we need to:
                 * a) validate their email address
                 * b) confirm their email address is not in use
                 */
                if (!isSignedIn && hasBasicEmailValidity) {
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
                    }).fail(function() {
                        validity.emailMessage = MESSAGES.emailFailure;
                        resolve(validity);
                    });
                } else {
                    resolve(validity);
                }

            } else {
                resolve(validity);
            }



        });
    };

});
