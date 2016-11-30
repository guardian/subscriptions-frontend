define([
    '$',
    'bean',
    'utils/ajax',
    'utils/text',
    'utils/serializer',
    'modules/forms/loader',
    'modules/forms/toggleError',
    'modules/checkout/formElements'
], function ($,
             bean,
             ajax,
             textUtils,
             serializer,
             loader,
             toggleError,
             formEls) {
    'use strict';

    function clickHelper($elem, callback) {
        if ($elem.length) {
            bean.on($elem[0], 'click', function (e) {
                e.preventDefault();
                callback();
            });
        }
    }

    function populateDetails() {
        formEls.$REVIEW_NAME.text(textUtils.mergeValues([
            formEls.$TITLE.val(),
            formEls.$FIRST_NAME.val(),
            formEls.$LAST_NAME.val()
        ], ' '));

        var BILLING_COUNTRY_SELECT = formEls.BILLING.$COUNTRY_SELECT[0];
        if (BILLING_COUNTRY_SELECT.disabled) {
            formEls.$REVIEW_ADDRESS.text('Same as above');
        } else {
            formEls.$REVIEW_ADDRESS.text(textUtils.mergeValues([
                formEls.BILLING.$ADDRESS1.val(),
                formEls.BILLING.$ADDRESS2.val(),
                formEls.BILLING.$TOWN.val(),
                formEls.BILLING.getSubdivision$().val(),
                formEls.BILLING.getPostcode$().val(),
                BILLING_COUNTRY_SELECT.options[BILLING_COUNTRY_SELECT.selectedIndex].text
            ], ', '));
        }

        var DELIVERY_COUNTRY_SELECT = formEls.DELIVERY.$COUNTRY_SELECT[0];

        formEls.$REVIEW_DELIVERY_ADDRESS.text(textUtils.mergeValues([
            formEls.DELIVERY.$ADDRESS1.val(),
            formEls.DELIVERY.$ADDRESS2.val(),
            formEls.DELIVERY.$TOWN.val(),
            formEls.DELIVERY.getSubdivision$().val(),
            formEls.DELIVERY.getPostcode$().val(),
            DELIVERY_COUNTRY_SELECT.options[DELIVERY_COUNTRY_SELECT.selectedIndex].text
        ], ', '));

        formEls.$REVIEW_EMAIL.text(formEls.$EMAIL.val());

        if (formEls.$PHONE.val().length > 0) {
            formEls.$REVIEW_PHONE_FIELD.show();
            formEls.$REVIEW_PHONE.text(formEls.$PHONE.val());
        } else {
            formEls.$REVIEW_PHONE_FIELD.hide();
        }


        formEls.$REVIEW_ACCOUNT.text(formEls.$ACCOUNT.val());
        formEls.$REVIEW_SORTCODE.text(formEls.$SORTCODE.val());
        formEls.$REVIEW_HOLDER.text(formEls.$HOLDER.val());
        formEls.$REVIEW_DELIVERY_INSTRUCTIONS.text(formEls.getDeliveryInstructions().val());
        formEls.$REVIEW_DELIVERY_START_DATE.text(formEls.getPaperCheckoutField().val());
    }

    function registerPopulateDetails() {
        clickHelper(formEls.$PAYMENT_DETAILS_SUBMIT, populateDetails);
    }


    function init() {
        registerPopulateDetails();
    }

    return {
        init: init,
        repopulateDetails: populateDetails
    };

});
