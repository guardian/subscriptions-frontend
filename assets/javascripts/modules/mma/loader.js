define(['$'],function($){
    'use strict';
    var $UPDATE = $('.js-payment-update');

    function init(){
        if($UPDATE.length){
            require(['modules/mma/updatePayment.jsx'],function(mma){
                curl('js!stripeCheckout').then(function(){
                    mma.init($UPDATE[0]);
                    //init();
                });
            });
        }
    }

    return{
        init:init
    }

});
