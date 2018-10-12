define(['utils/base64'], function (base64) {
    'use strict';

    function setCookie(name, value, days, isUnSecure, trimSubdomain) {
        var date;
        var expires;
        // used for testing purposes, cookies are secure by default
        var secureCookieString = isUnSecure ? '' : '; secure';

        if (days) {
            date = new Date();
            date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
            expires = '; expires=' + date.toGMTString();
        } else {
            expires = '';
        }

        var domain = trimSubdomain ? '; domain=' + getShortDomain() : '';

        document.cookie = [name, '=', value, expires, '; path=/', secureCookieString, domain ].join('');
    }

    // Trim subdomains for prod, code and dev.
    function getShortDomain() {
        const domain = document.domain || '';
        return domain.replace(/^(sub|subscribe)\./, '.');
    }

    function getCookie(name) {
        var nameEQ = name + '=';
        var ca = document.cookie.split(';');
        for (var i = 0; i < ca.length; i++) {
            var c = ca[i];
            while (c.charAt(0) === ' ') { c = c.substring(1, c.length); }
            if (c.indexOf(nameEQ) === 0) { return c.substring(nameEQ.length, c.length); }
        }
        return null;
    }

    function removeCookie(name) {
        setCookie(name, '', -1);
    }

    function getDecodedCookie(name) {
        return decodeCookie(getCookie(name));
    }

    function decodeCookie(cookieData) {
        var cookieVal = cookieData ? base64.decode(cookieData.split('.')[0]) : undefined;
        return (cookieVal) ? JSON.parse(cookieVal) : undefined;
    }

    return {
        setCookie: setCookie,
        getCookie: getCookie,
        removeCookie: removeCookie,
        getDecodedCookie: getDecodedCookie,
        decodeCookie: decodeCookie
    };

});
