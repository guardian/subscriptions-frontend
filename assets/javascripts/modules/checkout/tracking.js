define(['$', 'modules/analytics/ga', 'modules/analytics/omniture', 'modules/analytics/ophan', 'modules/checkout/ratePlanChoice'], function ($, ga, omniture, ophan, ratePlanChoice) {
    'use strict';

    function trackOmniture(omnnitureSlugName, omniturePageInfoName) {
        guardian.pageInfo.productData.eventName = 'scOpen';
        guardian.pageInfo.productData.source = 'Subscriptions and Membership';
        guardian.pageInfo.productData.type = 'GUARDIAN_DIGIPACK';
        if (omnnitureSlugName) {
            guardian.pageInfo.slug = 'GuardianDigiPack:'+omnnitureSlugName;
        }
        if (omniturePageInfoName) {
            guardian.pageInfo.name = 'Details - ' + omniturePageInfoName + ' | Digital | Subscriptions | The Guardian';
        }
        omniture.triggerPageLoadEvent();
    }

    function trackGA(googleAnalyticsEventName) {
        ga.trackEvent(googleAnalyticsEventName);
    }

    function tracking(omnnitureSlugName, omniturePageInfoName, googleAnalyticsEventName) {
        return function() {
            trackOmniture(omnnitureSlugName, omniturePageInfoName);
            trackGA(googleAnalyticsEventName);
        };
    }

    function trackRatePlanChange() {
        var selectedRatePlanData = ratePlanChoice.getSelectedOptionData();
        if (selectedRatePlanData) {
            guardian.pageInfo.productData.amount = selectedRatePlanData.amount;
            guardian.pageInfo.productData.productPurchasing = selectedRatePlanData.name;
            guardian.pageInfo.productData.numberOfMonths = selectedRatePlanData.numberOfMonths;
        }
        tracking('','','Rate Plan Change')();
    }

    return {
        personalDetailsTracking: tracking('Name and address', '', 'Your details'),
        paymentDetailsTracking: tracking('Payment Details', 'payment details', 'Payment details'),
        billingDetailsTracking: tracking('Billing Details', 'billing details', 'Billing address'),
        paymentReviewTracking: tracking('Review and confirm', 'submission/signup', 'Review and confirm'),
        deliveryDetailsTracking: tracking('Delivery address', 'delivery details', 'Delivery address'),
        init: function () {
            ratePlanChoice.registerOnChangeAction(trackRatePlanChange);
        }
    };
});
