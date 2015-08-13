define(function () {
    'use strict';

    var omniture = {};

    function sendEvent(prop17, products) {
        if (prop17) {
            omniture.prop17 = prop17;
        }
        if (products) {
            omniture.products = products;
        }
        if (prop17 && products) {
            omniture.t();
        }
    }

    function countrySelectTracking(qty, amount){
        var prop17 = 'GuardianDigiPack:Select Country',
            products = 'Subscriptions and Membership;GUARDIAN_DIGIPACK;' + qty + ';' + amount + ';scOpen';
        sendEvent(prop17, products);
    }

    function personalDetailsTracking(qty, amount){
        var prop17 = 'GuardianDigiPack:Name and address',
            products = 'Subscriptions and Membership;GUARDIAN_DIGIPACK;' + qty + ';' + amount + ';scOpen';
        sendEvent(prop17, products);
    }

    function paymentDetailsTracking(){
        sendEvent('GuardianDigiPack:Payment Details');
    }

    function paymentSubmissionTracking(qty, amount){
        var prop17 = 'GuardianDigiPack:Review and confirm',
            products = 'Subscriptions and Membership;GUARDIAN_DIGIPACK;' + qty + ';' + amount + ';scCheckout';
        sendEvent(prop17, products);
    }

    function subscriptionConfirmationTracking(qty, amount){
        var prop17 = 'GuardianDigiPack:Order Complete',
            products = 'Subscriptions and Membership;GUARDIAN_DIGIPACK;' + qty + ';' + amount + ';scCheckout';
        sendEvent(prop17, products);
    }

    function init(o){
        omniture = o;
    }

    return {
        init: init,
        countrySelectTracking: countrySelectTracking,
        personalDetailsTracking: personalDetailsTracking,
        paymentDetailsTracking: paymentDetailsTracking,
        paymentSubmissionTracking: paymentSubmissionTracking,
        subscriptionConfirmationTracking: subscriptionConfirmationTracking
    };

});
