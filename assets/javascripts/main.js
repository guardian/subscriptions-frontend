require([
    'utils/ajax',
    'modules/analytics/setup',
    'modules/toggle',
    'modules/optionMirror',
    'modules/appendAround',
    'modules/password',
    'modules/inputMask',
    'modules/checkout',
    'modules/country',
    'modules/confirmation',
    'modules/patterns',
    // Add new dependencies ABOVE this
    'Promise',
    'raven'
], function (ajax,
             analytics,
             toggle,
             appendAround,
             optionMirror,
             password,
             inputMask,
             checkout,
             country,
             confirmation,
             patterns) {
    'use strict';

    /**
     * Set up Raven, which speaks to Sentry to track errors
     */
    /*global Raven */
    Raven.config('https://6dd79da86ec54339b403277d8baac7c8@app.getsentry.com/47380', {
        whitelistUrls: ['subscribe.theguardian.com/assets/'],
        tags: {build_number: guardian.buildNumber}
    }).install();


    ajax.init({page: {ajaxUrl: ''}});

    analytics.init();

    toggle.init();
    optionMirror.init();
    appendAround.init();
    password.init();

        inputMask.init();
        country.init();
        checkout.init();
        confirmation.init();

    // Pattern library
    patterns.init();

});
