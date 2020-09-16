require([
    'utils/ajax',
    'modules/raven',
    'modules/analytics/setup',
    'modules/toggle',
    'modules/appendAround',
    'modules/profileMenu',
    'modules/userDetails',
    'modules/optionSwitch',
    'modules/inputMask',
    'modules/checkout',
    'modules/reportDeliveryProblem',
    'modules/suspend',
    'modules/confirmation',
    'modules/patterns',
    'modules/cas/form',
    'modules/renew/loader',
    'modules/animatedDropdown',
    'modules/index-intl',
    'object-fit-images',
    '@guardian/consent-management-platform'
], function (
    ajax,
    raven,
    analytics,
    toggle,
    appendAround,
    profileMenu,
    userDetails,
    optionSwitch,
    inputMask,
    checkout,
    reportDeliveryProblem,
    suspend,
    confirmation,
    patterns,
    cas,
    renew,
    dropdown,
    indexIntl,
    objectFitImages,
    cmp
) {
    'use strict';

    const PRIVACY_SETTINGS_SELECTOR = '.js-privacy-settings-link';
    const HIDDEN_CLASS = 'is-hidden';


    function createPrivacySettingsLink() {
        const privacySettingsButton = document.querySelector(PRIVACY_SETTINGS_SELECTOR);
        if (privacySettingsButton) {
            privacySettingsButton.classList.remove(HIDDEN_CLASS);
            privacySettingsButton.addEventListener('click', cmp.cmp.showPrivacyManager);
        }
    }

    // Get country to initialise CMP library
    fetch('/geocountry').then(response => {
        if (response.ok) {
            return response.text();
        } else {
            throw new Error('failed to get geocountry');
        }
    }).then(responseCountryCode => {
        cmp.cmp.init({
            isInUsa: responseCountryCode === 'US'
        });
        createPrivacySettingsLink();
    }).catch(err => {
        raven.Raven.captureException(err);
    });

    ajax.init({page: {ajaxUrl: ''}});
    raven.init('https://df7232e9685946ce965f2098ac3bdab2@sentry.io/1218847');
    analytics.init();
    toggle.init();
    appendAround.init();
    profileMenu.init();
    userDetails.init();
    optionSwitch.init();
    inputMask.init();
    checkout.init();
    reportDeliveryProblem.init();
    suspend.init();
    confirmation.init();
    cas.init();
    dropdown.init();
    indexIntl.init();
    objectFitImages();
    patterns.init();
    renew.init();
});
