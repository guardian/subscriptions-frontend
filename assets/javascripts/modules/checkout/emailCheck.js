define(['utils/ajax'], function (ajax) {
    'use strict';

    return function(email) {
        return ajax({ url: '/checkout/check-identity?email=' + email }).then(function (response) {
            return response.emailInUse;
        }).fail(function (err) {
            Raven.captureException(err);
        });
    };

});
