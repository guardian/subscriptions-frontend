define([
    'bean',
    'utils/ajax',
    'utils/text',
    'modules/checkout/formElements'
], function (bean, ajax, textUtils, formEls) {
    'use strict';

    function clickHelper($elem, callback) {
        if($elem.length) {
            bean.on($elem[0], 'click', function (e) {
                e.preventDefault();
                callback();
            });
        }
    }

    function populateDetails() {
        clickHelper(formEls.$PAYMENT_DETAILS_SUBMIT, function() {
            formEls.$REVIEW_NAME.text(textUtils.mergeValues([
                formEls.$FIRST_NAME.val(),
                formEls.$LAST_NAME.val()
            ], ' '));

            formEls.$REVIEW_ADDRESS.text(textUtils.mergeValues([
                formEls.$ADDRESS1.val(),
                formEls.$ADDRESS2.val(),
                formEls.$ADDRESS3.val(),
                formEls.$POSTCODE.val()
            ], ', '));

            formEls.$REVIEW_EMAIL.text(textUtils.mergeValues([
                formEls.$EMAIL.val()
            ], ''));

            formEls.$REVIEW_ACCOUNT.text(textUtils.mergeValues([
                formEls.$ACCOUNT.val()
            ], ''));

            formEls.$REVIEW_SORTCODE.text(textUtils.mergeValues([
                formEls.$SORTCODE1.val(),
                formEls.$SORTCODE2.val(),
                formEls.$SORTCODE3.val()
            ], '-'));

            formEls.$REVIEW_HOLDER.text(textUtils.mergeValues([
                formEls.$HOLDER.val()
            ], ''));
        });
    }

    function mandatePDF() {
        clickHelper(formEls.$MANDATE_PDF, function() {
            var formData = ajax.reqwest.serialize(formEls.$CHECKOUT_FORM[0]);
            ajax({
                method: 'POST',
                data: formData,
                url: '/checkout/mandate-pdf'
            }).then(function (resp) {
                if(resp && resp.mandatePDFUrl) {
                    window.open(resp.mandatePDFUrl);
                }
            }).catch(function (jsonError) {
                // TODO display error message to user
                Raven.captureException(jsonError);
            });
        });
    }

    function init() {
        populateDetails();
        mandatePDF();
    }

    return {
        init: init
    };

});
