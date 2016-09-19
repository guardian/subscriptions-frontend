define(['$', 'modules/analytics/omniture'], function ($,  omniture) {
    'use strict';

    function trackOmniture(slugName, pageInfoName) {
        guardian.pageInfo.productData.eventName = 'scOpen';
        guardian.pageInfo.productData.source = 'Subscriptions and Membership';
        guardian.pageInfo.productData.type = 'GUARDIAN_DIGIPACK';
        if (slugName) {
            guardian.pageInfo.slug = 'GuardianDigiPack:'+slugName;
        }
        if (pageInfoName) {
            guardian.pageInfo.name = 'Details - ' + pageInfoName + ' | Digital | Subscriptions | The Guardian';
        }
        omniture.triggerPageLoadEvent();
    }

    function trackImpression(slugName, pageInfoName) {
        return function() {
            trackOmniture(slugName, pageInfoName);
        };
    }

    return {
        personalDetailsTracking: trackImpression('Name and address', ''),
        paymentDetailsTracking: trackImpression('Payment Details', 'payment details'),
        billingDetailsTracking: trackImpression('Billing Details', 'billing details'),
        paymentReviewTracking: trackImpression('Review and confirm', 'submission/signup'),
        deliveryDetailsTracking: trackImpression('Delivery address', 'delivery details')
    };
});
