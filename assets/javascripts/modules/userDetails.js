define(['utils/user'], function (userUtil) {
    'use strict';

    var SELCTOR_PREFIX = '.js-user-';
    var AVAILABLE_DETAILS = ['displayname', 'firstName']

    function populateUserDetails(data) {
        AVAILABLE_DETAILS.forEach(function(detail) {
            var prop = data[detail];
            var el = document.querySelector(SELCTOR_PREFIX + detail);
            if(el && prop) {
                el.innerHTML = prop;
            }
        });
    }

    function init() {
        if (userUtil.isLoggedIn()) {
            populateUserDetails(userUtil.getUserFromCookie());
        }
    }

    return {
        init: init
    };
});
