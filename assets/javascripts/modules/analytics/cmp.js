import { onConsentChange, cmp } from '@guardian/consent-management-platform';

const PRIVACY_SETTINGS_SELECTOR = '.js-privacy-settings-link';
const HIDDEN_CLASS = 'is-hidden';

const getConsentForVendors = (cmpVendorIds) => new Promise((resolve) => {
    if (!Array.isArray(cmpVendorIds)) {
        return resolve({});
    }

    onConsentChange(state => {
        /**
         * Loop over cmpVendorIds and pull
         * vendor specific consent from state.
         */
        resolve(cmpVendorIds.reduce((accumulator, vendorId) => {
            const consented = state.tcfv2 && state.tcfv2.vendorConsents ? state.tcfv2.vendorConsents[vendorId] : null;
            return {
                ...accumulator,
                [vendorId]: consented,
            };
        }, {}));
    })
});

const checkAllTCFv2PurposesAreOptedIn = () => new Promise((resolve) => {
    onConsentChange(state => {
        resolve(state.tcfv2 && state.tcfv2.consents && Object.values(state.tcfv2.consents).every(Boolean));
    })
});

const checkCCPA = () => new Promise((resolve) => {
    onConsentChange(state => {
        resolve(state.ccpa ? !state.ccpa.doNotSell : null);
    })
});

const registerCallbackOnConsentChange = (fn) => onConsentChange(fn);

const createPrivacySettingsLink = () => {
    const privacySettingsButton = document.querySelector(PRIVACY_SETTINGS_SELECTOR);
    privacySettingsButton.classList.remove(HIDDEN_CLASS);
    privacySettingsButton.addEventListener('click', cmp.showPrivacyManager);
}

export { getConsentForVendors, checkAllTCFv2PurposesAreOptedIn, checkCCPA, registerCallbackOnConsentChange, createPrivacySettingsLink };