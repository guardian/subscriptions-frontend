define([
    'modules/analytics/analyticsEnabled',
    'modules/analytics/dntEnabled'
], function (analyticsEnabled, dntEnabled) {
    'use strict';

    var remarketingUrl = 'https://www.googleadservices.com/pagead/conversion_async.js';

    function init() {
        return curl(['js!' + remarketingUrl], function () {
            window.google_trackConversion({
                google_conversion_id: 971225648,
                google_custom_params: window.google_tag_params,
                google_remarketing_only: true
            });
            // JellyFish remarketing
            window.google_trackConversion({
                google_conversion_id: 995657703,
                google_custom_params: window.google_tag_params,
                google_remarketing_only: true
            });
        });
    }

    return {
        init: analyticsEnabled(dntEnabled(init))
    };
});
