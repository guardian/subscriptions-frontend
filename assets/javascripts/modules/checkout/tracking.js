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

    function trackEvent(prop17, pageName) {
        var products = subscriptionProducts();
        omniture.sendEvent(prop17, pageName, products);
    }

    function personalDetailsTracking(){
        trackEvent('GuardianDigiPack:Name and address', 'Details - name and address | Digital | Subscriptions | The Guardian');
    }

    function paymentDetailsTracking(){
        trackEvent('GuardianDigiPack:Payment Details', 'Details - payment details | Digital | Subscriptions | The Guardian');
    }

    function paymentSubmissionTracking(){
        trackEvent('GuardianDigiPack:Review and confirm', 'Payment submission/signup | Digital | Subscriptions | The Guardian');
    }

    function subscriptionCompleteTracking(){
        trackEvent('GuardianDigiPack:Order Complete', 'Confirmation | Digital | Subscriptions | The Guardian');
    }

    return {
        personalDetailsTracking: personalDetailsTracking,
        paymentDetailsTracking: paymentDetailsTracking,
        paymentSubmissionTracking: paymentSubmissionTracking,
        subscriptionCompleteTracking: subscriptionCompleteTracking
    };

});
