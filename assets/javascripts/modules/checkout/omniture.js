define(['bean', 'modules/checkout/formElements'], function (bean, formEls) {
    'use strict';

    function init(omniture){

        function sendEvent(prop17, pageName, products){
            if(prop17){
                omniture.prop17 = prop17;
            }
            if(products){
                omniture.products = products;
            }
            if(pageName){
                omniture.pageName = pageName;
            }
            if(pageName && prop17 && products){
                omniture.t();
            }
        }

        function personalDetailsShown(){
            var pageName = 'Details - name and address | Digital | Subscriptions | The Guardian',
                products = 'Subscriptions and Membership;GUARDIAN_DIGIPACK;<qty>;<amount>;scOpen',
                prop17 = 'GuardianDigiPack:Name and address';
            sendEvent(prop17, pageName, products);
        }

        var $yourDetailsSubmit = formEls.$YOUR_DETAILS_SUBMIT;
        var $editYourDetails = formEls.$EDIT_YOUR_DETAILS;

        if($yourDetailsSubmit.length && $editYourDetails.length){

            bean.on($yourDetailsSubmit[0], 'click', function (e) {
                e.preventDefault();
                personalDetailsShown();
            });

            bean.on($editYourDetails[0], 'click', function (e) {
                e.preventDefault();
                personalDetailsShown();
            });

        }

    }

    return {
        init: init
    };

});
