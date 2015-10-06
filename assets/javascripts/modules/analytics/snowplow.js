/*global snowplow_name_here,  */
define(['lodash/object'], function (_) {
    'use strict';

    var snowplow;

    function trackActivity(source, data) {
	var eventData = _.merge({
	    eventSource: source
	}, (data || {}));

	snowplow('trackUnstructEvent:subscriptions', eventData)
    }

    function init() {
	snowplow = snowplow_name_here;
	trackActivity('pageLoaded', {
	    title: document.title,
	    url: window.location.href
	});
	return snowplow;
    }

    return {
	init: init,
	trackActivity: trackActivity
    };
});
