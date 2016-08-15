define(['$', 'modules/analytics/omniture'], function ($, omniture) {
    'use strict';

    //TODO make this product aware

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

    function tracking(slugName, pageInfoName) {
        return function() {
            if (pageInfoName !== undefined) {
                guardian.pageInfo.name = 'Details - ' + pageInfoName + ' | Digital | Subscriptions | The Guardian';
            }
            guardian.pageInfo.slug = 'GuardianDigiPack:'+slugName;
            guardian.pageInfo.productData = subscriptionProducts('scOpen');
            omniture.triggerPageLoadEvent();
        };
    }

    return {
        personalDetailsTracking: tracking('Name and address'),
        paymentDetailsTracking: tracking('Payment Details', 'payment details'),
        billingDetailsTracking: tracking('Billing Details', 'billing details'),
        paymentReviewTracking: tracking('Review and confirm', 'submission/signup')
    };
});
