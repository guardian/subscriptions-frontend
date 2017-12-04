define([], function () {
    'use strict';

    return {
        isNumber: function isNumber(s) {
            return !/[^\d]+/.test(s);
        },
        isValidEmail: function isValidEmail(s) { // use regex from Scala Play (as is, so ignore eslint)
            //eslint-disable-next-line no-useless-escape
            return /^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/.test(s);
        },
        isPostcode: function isPostcode(s) {
            return /^[\sA-Z\d]{5,}$/i.test(s);
        }
    };
});
