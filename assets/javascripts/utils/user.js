define(['utils/cookie'], function(cookie){
    'use strict';

    var USER_COOKIE_KEY = 'GU_U';

    function isLoggedIn() {
        return !!getUserFromCookie();
    }

    function idCookieAdapter(data) {
        return {
            id: data[0],
            displayname: decodeURIComponent(data[2]),
            accountCreatedDate: data[6],
            emailVerified: data[7]
        };
    }

    function getUserFromCookie() {
        var userData = cookie.getDecodedCookie(USER_COOKIE_KEY);
        return (userData) ? idCookieAdapter(userData) : undefined;
    }

    return {
        isLoggedIn: isLoggedIn,
        getUserFromCookie: getUserFromCookie
    };
});
