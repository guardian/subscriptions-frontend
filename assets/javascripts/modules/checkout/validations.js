define(['$', 'bower_components/bean/bean', 'modules/checkout/formElements'], function ($, bean, form) {

    var validatePersonalDetails = function(){
        return false;
    };

    var validatePaymentDetails = function(){
        return true;
    };

    return {
        validatePersonalDetails: validatePersonalDetails,
        validatePaymentDetails: validatePaymentDetails
    }

});