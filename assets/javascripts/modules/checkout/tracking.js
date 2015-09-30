define(['$', 'modules/analytics/omniture'], function ($, omniture) {
    'use strict';

    function subscriptionProducts(eventName){
        var selectedFrequency = $('.js-payment-frequency input:checked');
        if (selectedFrequency.length && eventName) {
            var amount = selectedFrequency[0].getAttribute('data-amount'),
                qty = selectedFrequency[0].getAttribute('data-number-of-months');
            return 'Subscriptions and Membership;GUARDIAN_DIGIPACK;' + qty + ';' + amount + ';' + eventName;
        }
        return undefined;
    }

    function trackEvent(prop17, pageName, eventName) {
        var products = subscriptionProducts(eventName);
        omniture.sendEvent(prop17, pageName, products);
    }

    function personalDetailsTracking() {
        var prop17 = 'GuardianDigiPack:Name and address',
            pageName = 'Details - name and address | Digital | Subscriptions | The Guardian',
            eventName = 'scOpen';
        trackEvent(prop17, pageName, eventName);
    }

    function paymentDetailsTracking(){
        var prop17 = 'GuardianDigiPack:Payment Details',
            pageName = 'Details - payment details | Digital | Subscriptions | The Guardian';
        trackEvent(prop17, pageName);
    }

    function paymentSubmissionTracking(){
        var prop17 = 'GuardianDigiPack:Review and confirm',
            pageName = 'Payment submission/signup | Digital | Subscriptions | The Guardian';
        trackEvent(prop17, pageName, 'scCheckout');
    }

    function subscriptionCompleteTracking(){
        var prop17 = 'GuardianDigiPack:Order Complete',
            pageName = 'Confirmation | Digital | Subscriptions | The Guardian',
            eventName = 'purchase';
        trackEvent(prop17, pageName, eventName);
    }

    return {
        personalDetailsTracking: personalDetailsTracking,
        paymentDetailsTracking: paymentDetailsTracking,
        paymentSubmissionTracking: paymentSubmissionTracking,
        subscriptionCompleteTracking: subscriptionCompleteTracking
    };

});
