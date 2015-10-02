/*global guardian */
define([
    'modules/country/elements',
    'utils/cookie',
    'modules/analytics/snowplow',
    'bean'
], function (elements, cookie, snowplow, bean) {
    'use strict';

    var COUNTRY_FLOW_COOKIE_NAME = 'COUNTRY_FLOW';

    function shouldSwitch(percent) {
        return Math.floor(Math.random() * 101) <= percent;
    }

    function loadCountryInfo() {
        var countryFlowCookie = cookie.getCookie(COUNTRY_FLOW_COOKIE_NAME);
        return (countryFlowCookie) ? JSON.parse(countryFlowCookie) : null;
    }

    function switchUrl() {
        elements.$CHECKOUT_LINK.attr('href', elements.$CHECKOUT_LINK.attr('data-previous-href'));
    }

    function init() {
        guardian.pageInfo.productData = {
            source: 'Subscriptions and Membership',
            type: 'GUARDIAN_DIGIPACK',
            eventName: 'prodView'
        };
        guardian.pageInfo.slug = 'GuardianDigiPack:Select Country';

        var cookieInfo = loadCountryInfo();
        if (!cookieInfo) {
            cookieInfo = {switchUrl: shouldSwitch(50)};
            cookie.setCookie(COUNTRY_FLOW_COOKIE_NAME, JSON.stringify(cookieInfo));
        }
        if (cookieInfo.switchUrl) {
            switchUrl();
	    bean.on(elements.$CHECKOUT_LINK[0], 'click', function () {
		snowplow.trackActivity('redirectedToQss');
	    });
        }
    }

    return {
        init: init
    };

});
