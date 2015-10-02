/*global guardian:true */
define([], function () {
    'use strict';

    function trackActivity(source) {
	var snowplowNameHere = window.snowplow_name_here;
	snowplowNameHere('trackUnstructEvent:subscriptions', {
		eventSource: source
	    }
	)
    }

    function init() {
    }

    return {
	init: init,
	trackActivity: trackActivity
    };
});
