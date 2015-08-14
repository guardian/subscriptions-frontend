define(['$'], function ($) {
    'use strict';

    var omniture = {};

    function getSubscriptionInfo(){
        var selectedFrequency = $('.js-option-mirror-group input:checked');
        if (selectedFrequency.length) {
            var amount = selectedFrequency[0].getAttribute('data-amount'),
                qty = selectedFrequency[0].getAttribute('data-number-of-months');
            return 'Subscriptions and Membership;GUARDIAN_DIGIPACK;' + qty + ';' + amount + ';scOpen';
        }
        return false;
    }

    function sendEvent(prop17) {
        var products = getSubscriptionInfo();
        if (products) {
            omniture.products = products;
        }
        omniture.prop17 = prop17;
        omniture.t();
    }

    function personalDetailsTracking(){
        sendEvent('GuardianDigiPack:Name and address');
    }

    function paymentDetailsTracking(){
        sendEvent('GuardianDigiPack:Payment Details');
    }

    function paymentSubmissionTracking(){
        sendEvent('GuardianDigiPack:Review and confirm');
    }

    function init(o){
        omniture = o;
    }

    return {
        init: init,
        personalDetailsTracking: personalDetailsTracking,
        paymentDetailsTracking: paymentDetailsTracking,
        paymentSubmissionTracking: paymentSubmissionTracking
    };

});
