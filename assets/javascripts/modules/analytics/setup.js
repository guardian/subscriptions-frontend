define([
    'modules/analytics/ga',
    'modules/analytics/ophan',
    'modules/analytics/thirdPartyTracking'
], function (
    ga,
    ophan,
    thirdPartyTracking
) {
    'use strict';

    function init() {
        ophan.init();

        /**
         * GA and other tags often prefer Ophan to have bootstrapped and set window.guardian state,
         * so load them after Ophan has loaded if thirdPartyTrackingEnabled.
         */
        ophan.loaded.finally(() => {
            Promise.allSettled([
                thirdPartyTracking.checkCCPA(),
                thirdPartyTracking.getConsentForVendors([ga.sourcePointVendorId]),
                thirdPartyTracking.checkAllTCFv2PurposesAreOptedIn()
            ]).then(results => {
                const [ ccpaConsent, vendorConsents, allPurposesAgreed ] = results.map(promise => promise.value);

                if (ccpaConsent || vendorConsents[ga.sourcePointVendorId]) {
                    ga.init();
                } else {
                    if (ccpaConsent === null) {
                        console.log(`Either there's insufficient consent for Google Analytics, or the user has ` +
                            `turned that vendor off in the CMP (${ga.sourcePointVendorId}). ` +
                            `The user has ${allPurposesAgreed ? '' : 'not'} agreed to all purposes.`);
                    } else {
                        console.log(`Google Analytics (${ga.sourcePointVendorId}) not loaded due to CCPA opt-out`);
                    }
                }
            });
        });
    }

    return {
        init: init
    };
});
