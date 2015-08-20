define(['$', 'modules/analytics/omniture'], function ($, omniture) {
    'use strict';

    function subscriptionProducts(){
        var selectedFrequency = $('.js-payment-frequency input:checked');
        if (selectedFrequency.length) {
            var amount = selectedFrequency[0].getAttribute('data-amount'),
                qty = selectedFrequency[0].getAttribute('data-number-of-months');
            return 'Subscriptions and Membership;GUARDIAN_DIGIPACK;' + qty + ';' + amount + ';scOpen';
        }
        return false;
    }

    function trackEvent(prop17) {
        var products = subscriptionProducts();
        omniture.sendEvent(prop17, products);
    }

    function personalDetailsTracking(){
        trackEvent('GuardianDigiPack:Name and address');
    }

    function paymentDetailsTracking(){
        trackEvent('GuardianDigiPack:Payment Details');
    }

    function paymentSubmissionTracking(){
        trackEvent('GuardianDigiPack:Review and confirm');
    }

    function subscriptionCompleteTracking(){
        trackEvent('GuardianDigiPack:Order Complete');
    }

    return {
        personalDetailsTracking: personalDetailsTracking,
        paymentDetailsTracking: paymentDetailsTracking,
        paymentSubmissionTracking: paymentSubmissionTracking,
        subscriptionCompleteTracking: subscriptionCompleteTracking
    };

});
