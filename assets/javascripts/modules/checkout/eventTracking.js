/* global guardian */
define(['modules/analytics/ga', 'modules/checkout/ratePlanChoice'], function (ga, ratePlanChoice) {
    'use strict';

    var STARTED = Date.now();

    function getElapsedSeconds() {
        return Math.floor((Date.now() - STARTED) / 1000);
    }

    function trackEvent(eventName) {
        return function () {
            ga.trackEvent(eventName, getElapsedSeconds());
        };
    }

    function trackRatePlanChange() {
        var selectedRatePlanData = ratePlanChoice.getSelectedOptionData();
        if (selectedRatePlanData) {
            guardian.pageInfo.productData.amount = selectedRatePlanData.amount;
            guardian.pageInfo.productData.productPurchasing = selectedRatePlanData.name;
        }
        trackEvent('Rate Plan Change')();
    }

    return {
        completedPersonalDetails: trackEvent('Your details'),
        completedBillingDetails: trackEvent('Billing address'),
        completedPaymentDetails: trackEvent('Payment details'),
        completedReviewDetails: trackEvent('Review and confirm'),
        completedDeliveryDetails: trackEvent('Delivery address'),
        init: function () {
            ratePlanChoice.registerOnChangeAction(trackRatePlanChange);

            // Trigger the Converted metric if the product was already purchased upon page initialisation.
            if (guardian.pageInfo.productData.productPurchased) {
                trackEvent('Converted')();
            }
        }
    };
});
