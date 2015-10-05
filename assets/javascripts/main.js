require([
    'utils/ajax',
    'modules/raven',
    'modules/analytics/setup',
    'modules/toggle',
    'modules/appendAround',
    'modules/controls',
    'modules/userDetails',
    'modules/optionMirror',
    'modules/password',
    'modules/inputMask',
    'modules/checkout',
    'modules/country',
    'modules/confirmation',
    'modules/patterns',
    // Add new dependencies ABOVE this
    'Promise'
], function (
    ajax,
    raven,
    analytics,
    toggle,
    appendAround,
    controls,
    userDetails,
    optionMirror,
    password,
    inputMask,
    checkout,
    country,
    confirmation,
    patterns
) {
    'use strict';

    ajax.init({page: {ajaxUrl: ''}});
    raven.init('https://6dd79da86ec54339b403277d8baac7c8@app.getsentry.com/47380');
    analytics.init();

    toggle.init();
    appendAround.init();
    controls.init();
    userDetails.init();
    optionMirror.init();
    password.init();
    inputMask.init();
    country.init();
    checkout.init();
    confirmation.init();
    patterns.init();

});
