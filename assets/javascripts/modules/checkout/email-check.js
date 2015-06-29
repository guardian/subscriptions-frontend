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
            if (guardian.user.isSignedIn) { resolve(); return; }

            var email = form.$EMAIL && textUtils.removeWhitespace(form.$EMAIL.val());
            if (email) {
                ajax({
                    url: '/checkout/check-identity?email=' + email
                }).then(function (response) {
                    if (response.emailInUse) {
                        var msg = 'Your email is already in use! Please sign in or use another email address';
                        reject(new Error(msg));
                    } else {
                        resolve();
                    }
                }).fail(function (_, msg) {
                    reject(new Error('Error reaching endpoint /checkout/check-identity:' + msg));
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
