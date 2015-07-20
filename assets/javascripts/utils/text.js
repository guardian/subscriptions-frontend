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
        }
    };
});
