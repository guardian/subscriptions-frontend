define(['$'],function($){
    'use strict';
    var $WEEKLY = $('.js-weekly-renew');

    function init(){
        if($WEEKLY.length){
           require(['modules/react/weeklyRenew.jsx'],function(weeklyRenew){
               curl('js!stripeCheckout').then(function(){
                   weeklyRenew.init($WEEKLY[0]);
           });
            });
        }
    }

    return{
        init:init
    }

});
