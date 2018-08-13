/* global ga */
define(['$',
    'bean',
], function (
    $,
    bean,
) {
    'use strict';

    function trackClick(eventLabel) {
        if (typeof(ga) === 'undefined') { return; }

        var event = {
            eventCategory: 'click',
            eventAction:  'international_subs_landing_pages',
            eventLabel: eventLabel,
        };
        ga('membershipPropertyTracker.send', 'event', event);
    }

    function addClickTracking(selector, eventLabel) {
        var $actionEl = $(selector);
        if ($actionEl.length) {
            bean.on($actionEl[0], 'click', () => trackClick(eventLabel));
        }
    }

    function init() {
        addClickTracking('.js-index-intl-weekly-cta', 'weekly_cta');
        addClickTracking('.js-index-intl-digipack-cta', 'digipack_cta');
    }

    return {
        init: init
    };
});
