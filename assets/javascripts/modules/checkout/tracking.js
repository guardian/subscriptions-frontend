define(['$', 'modules/analytics/omniture', 'modules/analytics/snowplow'], function ($, omniture, snowplow) {
    'use strict';

    function subscriptionProducts(eventName) {
        var selectedFrequency = $('.js-payment-frequency input:checked');
        if (selectedFrequency.length && eventName) {
            var amount = selectedFrequency[0].getAttribute('data-amount'),
                qty = selectedFrequency[0].getAttribute('data-number-of-months');
            return {
                source: 'Subscriptions and Membership',
                type: 'GUARDIAN_DIGIPACK',
                eventName: eventName,
                amount: amount,
                frequency: qty
            };
        }
        return undefined;
    }

    function personalDetailsTracking() {
        guardian.pageInfo.slug = 'GuardianDigiPack:Name and address';
        guardian.pageInfo.productData = subscriptionProducts('scOpen');
        omniture.triggerPageLoadEvent();
	snowplow.trackPageLoad();
    }

    function paymentDetailsTracking() {
	guardian.pageInfo.slug = 'GuardianDigiPack:Payment Details';
	guardian.pageInfo.name = 'Details - payment details | Digital | Subscriptions | The Guardian';
	guardian.pageInfo.productData = subscriptionProducts('scOpen');
	omniture.triggerPageLoadEvent();
	snowplow.trackPageLoad();
    }

    function paymentReviewTracking() {
	guardian.pageInfo.slug = 'GuardianDigiPack:Review and confirm';
	guardian.pageInfo.name = 'Payment submission/signup | Digital | Subscriptions | The Guardian';
	guardian.pageInfo.productData = subscriptionProducts('scCheckout');
	omniture.triggerPageLoadEvent();
	snowplow.trackPageLoad();
    }

    return {
        personalDetailsTracking: personalDetailsTracking,
        paymentDetailsTracking: paymentDetailsTracking,
        paymentReviewTracking: paymentReviewTracking
    };

});
