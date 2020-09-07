define([
    'modules/analytics/ga',
    'modules/analytics/ophan',
    'modules/analytics/cmp'
], function (
    ga,
    ophan,
    cmp
) {
    'use strict';

    function loadGA() {
        Promise.allSettled([
            cmp.checkCCPA(),
            cmp.getConsentForVendors([ga.cmpVendorId]),
            cmp.checkAllTCFv2PurposesAreOptedIn()
        ]).then(results => {
            const [ ccpaConsent, vendorConsents, allPurposesAgreed ] = results.map(promise => promise.value);

            if (ccpaConsent || vendorConsents[ga.cmpVendorId]) {
                ga.init();
                loadGA.complete = true;
            } else if (allPurposesAgreed && typeof(vendorConsents[ga.cmpVendorId]) === 'undefined') {
                console.log('Google Analytics has not been configured as a vendor yet, but all purposes have been ' +
                    'agreed so we\'re loading it.');
                ga.init();
                loadGA.complete = true;
            } else {
                if (ccpaConsent === null) {
                    console.log('Either there\'s insufficient consent for Google Analytics, or the user has ' +
                        `turned that vendor off in the CMP (${ga.cmpVendorId}). ` +
                        `The user has ${allPurposesAgreed ? '' : 'not '}agreed to all purposes.`);
                } else {
                    console.log(`Google Analytics (${ga.cmpVendorId}) not loaded due to CCPA opt-out`);
                }
            }
        });
    }

    function init() {
        ophan.init();

        /**
         * GA and other tags often prefer Ophan to have bootstrapped and set window.guardian state,
         * so load them after Ophan has loaded if thirdPartyTrackingEnabled.
         */
        ophan.loaded.finally(() => {
            cmp.createPrivacySettingsLink();
            cmp.registerCallbackOnConsentChange(loadGA)
        });
    }

    return {
        init: init
    };
});
