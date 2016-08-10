define([
    'utils/ajax',
    'modules/checkout/formElements',
    'modules/forms/regex',
    'modules/raven'
], function (ajax, formElements, regex, raven) {
    'use strict';

    function emailCheck(email) {
        return ajax({
            method: 'GET',
            type: 'json',
            url: formElements.$EMAIL.data('validation-url'),
            data: {
                'email': email
            }
        }).then(function (response) {
            return response.emailInUse;
        }).fail(function (err) {
            raven.Raven.captureException(err);
        });
    }

    return function(data, isSignedIn) {

        var MESSAGES = {
            emailInvalid: 'Please enter a valid email address.',
            emailTaken: 'You already have a Guardian account. Please <a class="u-nowrap" href="' + formElements.$SIGN_IN_LINK.attr('href') + '">sign in</a> or use another email address.',
            emailFailure: 'Sorry there has been a problem. Please try again later.'
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

            var hasBasicEmailValidity = (
                hasValidEmail &&
                hasConfirmedEmail
            );

            var validity = {
                allValid: false,
                requiredFieldValues: data.requiredFieldValues,
                hasValidEmail: hasValidEmail,
                hasConfirmedEmail: hasConfirmedEmail,
                emailMessage: hasValidEmail ? false : MESSAGES.emailInvalid,
                isEmailInUse: false
            };


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
        });
    };
});
