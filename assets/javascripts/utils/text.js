define(function() {
    'use strict';

    return {
        trimWhitespace: function(str) {
            return str.replace(/(^\s+|\s+$)/g, '');
        },
        removeWhitespace: function(str) {
            return str.replace(/\s+/g, '');
        },
        /**
         * 2. takes array of values
         * 3. Filters out empty items
         * 4. Joins with provided separator
         */
        mergeValues: function (values, separator) {
            return values.filter(function(val) {
                return val !== '';
            }).join(separator);
        },
        /**
         * For credit/debit card number.
         * Replaces all digits except for last n with chars.
         * Does not modify string in place but returns a new string.
         */
        obscure: function(str, n, chars) {
            var replace = str.slice(0, str.length-n);
            var leaveIntact = n === 0 ? '' : str.slice(-n);

            return replace.replace(/\d/g, chars) + leaveIntact;
        }
    };
});
