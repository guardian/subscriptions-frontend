define([], function () {
    return {
        isNumber: function isNumber(s) {
            return ! /[^\d]+/.test(s);
        },
        isValidEmail: function isValidEmail(s) {
            return /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(s);
        },
        isPostcode: function isPostcode(s) {
            return /^[\sA-Z\d]{5,}$/i.test(s);
        }
    }
});