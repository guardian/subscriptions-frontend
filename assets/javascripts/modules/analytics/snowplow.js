/*global snowplow_name_here, guardian */
define(['lodash/object'], function (_) {
    'use strict';

    var snowplow;

    var schema = {
	'$schema': 'http://json-schema.org/schema#',
	'self': {
	    'vendor': 'com.gu',
	    'name': 'page_load',
	    'format': 'jsonschema',
	    'version': '1-0-0'
	},
	'type': 'object',
	'properties': {
	    'eventSource': {
		'type': 'string'
	    },
	    'pageName': {
		'type': 'string'
	    },
	    'channel': {
		'type': 'string'
	    },
	    'productBillingFrequency': {
		'type': 'string'
	    },
	    'productBillingAmount': {
		'type': 'string'
	    },
	    'productType': {
		'type': 'string'
	    }
	},
	'required': ['eventSource', 'pageName', 'channel','productBillingFrequency','productBillingAmount','productType'],
	'additionalProperties': false
    };


    function trackActivity(source, data) {
	var eventData = _.merge({
	    eventSource: source
	}, (data || {}));

	snowplow('trackUnstructEvent', {schema: schema, data: eventData})
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
