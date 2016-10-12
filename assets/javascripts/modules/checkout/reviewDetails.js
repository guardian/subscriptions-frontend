define([
    'bean',
    'utils/ajax',
    'utils/text',
    'utils/serializer',
    'modules/forms/loader',
    'modules/forms/toggleError',
    'modules/checkout/formElements',
    'modules/checkout/payment',
    'modules/checkout/eventTracking',
    'modules/checkout/stripeErrorMessages',
    'modules/raven'
], function (bean,
             ajax,
             textUtils,
             serializer,
             loader,
             toggleError,
             formEls,
             payment,
             eventTracking,
             paymentErrorMessages,
             raven) {
    'use strict';

    var FIELDSET_COLLAPSED = 'is-collapsed';

    function clickHelper($elem, callback) {
        if ($elem.length) {
            bean.on($elem[0], 'click', function (e) {
                e.preventDefault();
                callback();
            });
        }
    }

    function populateDetails(token) {
        console.log('populatin details');
        console.log(token);
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

        var obscuredCardNumber = token ? "**** **** **** " + token.card.last4 : textUtils.obscure(formEls.$CARD_NUMBER.val(), 4, '*');
        formEls.$REVIEW_CARD_NUMBER.text(obscuredCardNumber);
        formEls.$REVIEW_CARD_EXPIRY.text(textUtils.mergeValues([
            token ? token.card.exp_month : formEls.$CARD_EXPIRY_MONTH.val(),
            token ? token.card.exp_year : formEls.$CARD_EXPIRY_YEAR.val()
        ], '/'));

        formEls.$REVIEW_DELIVERY_INSTRUCTIONS.text(formEls.getDeliveryInstructions().val());
        formEls.$REVIEW_DELIVERY_START_DATE.text(formEls.getPaperCheckoutField().val());
    }

    function registerPopulateDetails() {
        if (!guardian.stripeCheckout) {
            clickHelper(formEls.$PAYMENT_DETAILS_SUBMIT, populateDetails);
        }
    }

    function submitHandler() {
        var submitEl;
        if (formEls.$CHECKOUT_SUBMIT.length) {
            submitEl = formEls.$CHECKOUT_SUBMIT[0];

            var form = formEls.$CHECKOUT_FORM[0];
            form.addEventListener('submit', function (e) {
                e.preventDefault();

                loader.setLoaderElem(document.querySelector('.js-loader'));
                loader.startLoader();
                submitEl.setAttribute('disabled', 'disabled');

                var data = serializer([].slice.call(form.elements));


                ajax({
                    url: '/checkout',
                    method: 'post',
                    data: data,
                    success: function (successData) {
                        eventTracking.completedReviewDetails();
                        window.location.assign(successData.redirect);
                    },
                    error: function (err) {
                        loader.stopLoader();
                        submitEl.removeAttribute('disabled');

                        formEls.$FIELDSET_PAYMENT_DETAILS
                            .removeClass(FIELDSET_COLLAPSED);

                        formEls.$FIELDSET_REVIEW
                            .addClass(FIELDSET_COLLAPSED);

                        formEls.$FIELDSET_YOUR_DETAILS[0]
                            .scrollIntoView();

                        toggleError(formEls.$CARD_CONTAINER, true);

                        var paymentErr;

                        try {
                            paymentErr = JSON.parse(err.response);
                        } catch (e) {
                            raven.Raven.captureException(e);
                        }

                        var userMessage = paymentErrorMessages.getMessage(paymentErr);
                        var errorElement = paymentErrorMessages.getElement(paymentErr);
                        if (errorElement) {
                            errorElement.textContent = userMessage;
                        }
                    }
                });
            }, false);
        }

    }

    function init() {
        registerPopulateDetails();
        submitHandler();
    }

    return {
        init: init,
        repopulateDetails: populateDetails
    };

});
