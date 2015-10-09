/*global snowplow_name_here, guardian */
define(['lodash/object'], function (_) {
    'use strict';

    var snowplow;

    function trackActivity(source, data) {
	var eventData = _.merge({
	    eventSource: source
	}, (data || {}));

	snowplow('trackUnstructEvent', eventData)
    }

    function trackPageLoad() {
	var pageInfo = guardian.pageInfo,
	    productData = guardian.productData;

	var data = {
	    pageName: pageInfo.name,
	    channel: pageInfo.channel,
	    productBillingFrequency: '', //NOTE Seems that snowplow doesnt work without these default values and requiring in schema
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
	snowplow = snowplow_name_here;
	trackPageLoad();
	return snowplow;

    }

    return {
	init: init,
	trackActivity: trackActivity,
	trackPageLoad: trackPageLoad
    }
});
