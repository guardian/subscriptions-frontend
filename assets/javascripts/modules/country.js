define([
    'modules/country/elements',
    'utils/cookie'
], function (elements, cookie) {
    'use strict';

    var COUNTRY_FLOW_COOKIE_NAME = 'country-flow';

    function shouldSwitch() {
        return Math.floor(Math.random() * 2) === 1;
    }

    function getCountryFlowCookieInfo() {
        var countryFlowCookie = cookie.getCookie(COUNTRY_FLOW_COOKIE_NAME);
        return (countryFlowCookie) ? JSON.parse(countryFlowCookie) : null;

    }

    function switchUrl() {
        elements.$CHECKOUT_LINK.attr('href', elements.$CHECKOUT_LINK.attr('data-previous-href'));
    }

    function init() {
        var cookieInfo = getCountryFlowCookieInfo();
        if (!cookieInfo) {
            cookieInfo = {switchUrl: shouldSwitch()};
            cookie.setCookie(COUNTRY_FLOW_COOKIE_NAME, JSON.stringify(cookieInfo));
        }
        if (cookieInfo.switchUrl) {
            switchUrl();
        }
    }

    return {
        init: init
    };

});
