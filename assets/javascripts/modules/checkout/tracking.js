define(['$', 'modules/analytics/omniture'], function ($, omniture) {
    'use strict';

    function subscriptionProducts(eventName){
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
        guardian.slug = 'GuardianDigiPack:Name and address';
        guardian.productData = subscriptionProducts('scOpen');
        omniture.triggerPageLoadEvent();
    }

    function paymentDetailsTracking(){
        guardian.slug = 'GuardianDigiPack:Payment Details';
        guardian.pageName = 'Details - payment details | Digital | Subscriptions | The Guardian';
        guardian.productData = subscriptionProducts('scOpen');
        omniture.triggerPageLoadEvent();
    }

    function paymentReviewTracking(){
        guardian.slug = 'GuardianDigiPack:Review and confirm';
        guardian.pageName ='Payment submission/signup | Digital | Subscriptions | The Guardian';
        guardian.productData =  subscriptionProducts('scCheckout');
        omniture.triggerPageLoadEvent();
    }


    return {
        personalDetailsTracking: personalDetailsTracking,
        paymentDetailsTracking: paymentDetailsTracking,
        paymentReviewTracking: paymentReviewTracking
    };

});
