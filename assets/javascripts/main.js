require([
    'utils/ajax',
    'modules/raven',
    'modules/analytics/setup',
    'modules/toggle',
    'modules/appendAround',
    'modules/profileMenu',
    'modules/userDetails',
    'modules/optionSwitch',
    'modules/password',
    'modules/inputMask',
    'modules/checkout',
    'modules/confirmation',
    'modules/patterns',
    'modules/cas/form',
    // Add new dependencies ABOVE this
    'Promise'
], function (
    ajax,
    raven,
    analytics,
    toggle,
    appendAround,
    profileMenu,
    userDetails,
    optionSwitch,
    password,
    inputMask,
    checkout,
    confirmation,
    cas,
    patterns
) {
    'use strict';

    ajax.init({page: {ajaxUrl: ''}});
    raven.init('https://6dd79da86ec54339b403277d8baac7c8@app.getsentry.com/47380');
    analytics.init();

    toggle.init();
    appendAround.init();
    profileMenu.init();
    userDetails.init();
    optionSwitch.init();
    password.init();
    inputMask.init();
    checkout.init();
    confirmation.init();
    cas.init();
    patterns.init();
});
