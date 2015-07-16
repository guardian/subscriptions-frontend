require([
    'utils/ajax',
    'modules/toggle',
    'modules/appendAround',
    'modules/checkout/checkout',
    'modules/components/password',
    'modules/patterns',
    // Add new dependencies ABOVE this
    'raven'
], function(
    ajax,
    toggle,
    appendAround,
    checkout,
    password,
    patterns
) {
    'use strict';

    /**
     * Set up Raven, which speaks to Sentry to track errors
     */
    /*global Raven */
    Raven.config('https://6dd79da86ec54339b403277d8baac7c8@app.getsentry.com/47380', {
        whitelistUrls: ['subscribe.theguardian.com/assets/'],
        tags: { build_number: guardian.buildNumber }
    }).install();

    ajax.init({page: {ajaxUrl: ''}});

    toggle.init();
    appendAround.init();
    checkout.init();
    password.init();

    // Pattern library
    patterns.init();

});
