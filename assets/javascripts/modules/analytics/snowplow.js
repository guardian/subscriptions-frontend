/*global snowplow, guardian */
define(['lodash/object/merge'], function (merge) {
    'use strict';

    function loadSnowPlow() {
	if (!window.snowplow) {
	    window.GlobalSnowplowNamespace = window.GlobalSnowplowNamespace || [];
	    window.GlobalSnowplowNamespace.push('snowplow');
	    window.snowplow = function () {
		(window.snowplow.q = window.snowplow.q || []).push(arguments);
	    };
	    window.snowplow.q = window.snowplow.q || [];
	    window.snowplow('newTracker', 'subscriptions', guardian.trackerUrl, {
		appId: 'subscriptions-frontend'
	    });
	}
	return require('js!snowplow');
    }

    function trackActivity(source, data) {
	var eventData = merge({
	    eventSource: source
	}, (data || {}));

	loadSnowPlow().then(function () {
	    snowplow('trackUnstructEvent', eventData)
	});
    }

    function trackPageLoad() {
	var pageInfo = guardian.pageInfo,
	    productData = guardian.pageInfo.productData;

	var data = {
	    pageName: pageInfo.name,
	    channel: pageInfo.channel,
	    productBillingFrequency: '', //NOTE: Seems that snowplow doesn't work without these default values
	    productBillingAmount: '',
	    productType: ''
	};

	if (productData) {
	    data.productBillingFrequency = productData.frequency.toString();
	    data.productBillingAmount = productData.amount;
	    data.productType = productData.type;
	}

	trackActivity('pageLoaded', data);
    }

    function init() {
	if (!guardian.ignorePageLoadTracking) {
	    trackPageLoad();
	}
    }

    return {
	init: init,
	trackActivity: trackActivity,
	trackPageLoad: trackPageLoad
    }
});
