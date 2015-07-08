require([
    'utils/ajax',
    'modules/toggle',
    'modules/checkout/checkout',
    'modules/components/password',
    // Add new dependencies ABOVE this
    'raven'
], function(
    ajax,
    toggle,
    checkout,
    password
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
    checkout.init();
    password.init();
});
