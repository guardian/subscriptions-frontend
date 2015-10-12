define([
    'utils/cookie',
    'modules/analytics/ga',
    'modules/analytics/omniture',
    'modules/analytics/remarketing',
    'modules/analytics/krux',
    'modules/analytics/affectv',
    'modules/analytics/snowplow'
], function (cookie,
	     ga,
	     omniture,
	     remarketing,
	     krux,
	     affectv,
	     snowplow) {
    'use strict';

    function init() {
	var analyticsEnabled = (
	    window.guardian.analyticsEnabled &&
	    !navigator.doNotTrack &&
	    !cookie.getCookie('ANALYTICS_OFF_KEY')
	);

	if (analyticsEnabled) {
	    snowplow.init();
	    ga.init();
	    omniture.init();

            if (!window.guardian.isDev) {
                remarketing.init();
                krux.init();
                affectv.init();
            }
        }
    }

    return {
        init: init
    };
});
