import { onConsentChange } from '@guardian/consent-management-platform';

const getConsentForVendors = (sourcePointVendorIds) => new Promise((resolve) => {
    if (!Array.isArray(sourcePointVendorIds)) {
        return resolve({});
    }

    onConsentChange(state => {
        /**
         * Loop over sourcePointVendorIds and pull
         * vendor specific consent from state.
         */
        resolve(sourcePointVendorIds.reduce((accumulator, vendorKey) => {
            const vendorId = sourcePointVendorIds[vendorKey];
            if (
                state.tcfv2 &&
                state.tcfv2.vendorConsents &&
                state.tcfv2.vendorConsents[vendorId] !== undefined
            ) {
                return {
                    ...accumulator,
                    [vendorKey]: state.tcfv2.vendorConsents[vendorId],
                };
            } else {
                return {
                    ...accumulator,
                    [vendorKey]: false,
                };
            }
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

export { getConsentForVendors, checkAllTCFv2PurposesAreOptedIn, checkCCPA };
