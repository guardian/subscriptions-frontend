define([
    'bean',
    'utils/ajax',
    'utils/text',
    'modules/checkout/form-elements',
    'Promise'
], function (bean, ajax, textUtils, form) {
    'use strict';

    function warnIfEmailTaken() {
        return new Promise(function (resolve, reject) {
            if (guardian.user.isSignedIn) {
                resolve();
                return;
            }

            var email = form.$EMAIL && textUtils.removeWhitespace(form.$EMAIL.val());
            if (email) {
                ajax({
                    url: '/checkout/check-identity?email=' + email
                }).then(function (response) {
                    if (response.emailInUse) {
                        reject('EMAIL_IN_USE');
                    } else {
                        resolve();
                    }
                }).fail(function (_, msg) {
                    if(console){
                        console.error('Error reaching endpoint /checkout/check-identity:' + msg);
                    }
                    reject(new Error('NETWORK_FAILURE'));
                });

            } else {
                reject(new Error('Email is blank'));
            }
        });
    }

    return {
        warnIfEmailTaken: warnIfEmailTaken
    };
});
