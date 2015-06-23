define([
    '$',
    'bean',
    'modules/form/email-check'
], function ($, bean, emailCheck) {

    var $FIRST_NAME                 = $('.js-checkout-first'),
        $LAST_NAME                  = $('.js-checkout-last'),
        $EMAIL                      = $('.js-checkout-email'),
        $ADDRESS1                   = $('.js-checkout-house'),
        $ADDRESS2                   = $('.js-checkout-street'),
        $ADDRESS3                   = $('.js-checkout-town'),
        $POSTCODE                   = $('.js-checkout-postcode'),
        $ACCOUNT                    = $('.js-checkout-account'),
        $SORTCODE1                  = $('.js-checkout-sortcode1'),
        $SORTCODE2                  = $('.js-checkout-sortcode2'),
        $SORTCODE3                  = $('.js-checkout-sortcode3'),
        $HOLDER                     = $('.js-checkout-holder'),
        $FIND_ADDRESS               = $('.js-checkout-find-address'),
        $MANUAL_ADDRESS             = $('.js-checkout-manual-address'),
        $FULL_ADDRESS               = $('.js-checkout-full-address'),
        $YOUR_DETAILS_SUBMIT        = $('.js-checkout-your-details-submit'),
        $PAYMENT_DETAILS_SUBMIT     = $('.js-checkout-payment-details-submit'),
        $REVIEW_NAME                = $('.js-checkout-review-name'),
        $REVIEW_ADDRESS             = $('.js-checkout-review-address'),
        $REVIEW_EMAIL               = $('.js-checkout-review-email'),
        $REVIEW_ACCOUNT             = $('.js-checkout-review-account'),
        $REVIEW_SORTCODE            = $('.js-checkout-review-sortcode'),
        $REVIEW_HOLDER              = $('.js-checkout-review-holder'),
        $SMALLPRINT                 = $('.js-checkout-smallprint'),
        $FIELDSET_YOUR_DETAILS      = $('.js-fieldset-your-details'),
        $FIELDSET_PAYMENT_DETAILS   = $('.js-fieldset-payment-details'),
        $FIELDSET_REVIEW            = $('.js-fieldset-review'),
        $EDIT_YOUR_DETAILS          = $('.js-edit-your-details'),
        $EDIT_PAYMENT_DETAILS       = $('.js-edit-payment-details'),

        FIELDSET_COLLAPSED = 'fieldset--collapsed',
        FIELDSET_COMPLETE = 'data-fieldset-complete',
        IS_HIDDEN = 'is-hidden';

    var findAddress = function () {
        if($FIND_ADDRESS.length > 0){
            bean.on($FIND_ADDRESS[0], 'click', function (e) {
                e.preventDefault();

                if ($POSTCODE.val()) {
                    // TODO: Ajax to lookup service
                    populateAddressFields({
                        'house': 'Flat 14 Bankside House',
                        'street': 'West Hill',
                        'town': 'Putney'
                    });
                    showFullAddressFields();
                }
            });
        }
    };

    var populateAddressFields = function (address) {
        $ADDRESS1.val(address.house);
        $ADDRESS2.val(address.street);
        $ADDRESS3.val(address.town);
    };

    var manualAddress = function () {
        if($MANUAL_ADDRESS.length > 0){
            bean.on($MANUAL_ADDRESS[0], 'click', function (e) {
                e.preventDefault();
                showFullAddressFields();
                $MANUAL_ADDRESS.addClass(IS_HIDDEN);
            });
        }
    };

    var showFullAddressFields = function () {
        $FULL_ADDRESS.removeClass(IS_HIDDEN);
    };

    var toggleFieldsets = function () {
        if($YOUR_DETAILS_SUBMIT.length > 0){
            bean.on($YOUR_DETAILS_SUBMIT[0], 'click', function (e) {
                e.preventDefault();
                $FIELDSET_YOUR_DETAILS.addClass(FIELDSET_COLLAPSED).attr(FIELDSET_COMPLETE, '');
                $FIELDSET_PAYMENT_DETAILS.removeClass(FIELDSET_COLLAPSED);
                $EDIT_YOUR_DETAILS.removeClass(IS_HIDDEN);
                $EDIT_PAYMENT_DETAILS.addClass(IS_HIDDEN);
            });
        }

        if($PAYMENT_DETAILS_SUBMIT.length > 0){
            bean.on($PAYMENT_DETAILS_SUBMIT[0], 'click', function (e) {
                e.preventDefault();
                $FIELDSET_PAYMENT_DETAILS.addClass(FIELDSET_COLLAPSED).attr(FIELDSET_COMPLETE, '');
                $FIELDSET_REVIEW.removeClass(FIELDSET_COLLAPSED);
                $EDIT_PAYMENT_DETAILS.removeClass(IS_HIDDEN);
            });
        }

        if($EDIT_YOUR_DETAILS.length > 0){
            bean.on($EDIT_YOUR_DETAILS[0], 'click', function (e) {
                e.preventDefault();
                collapseFieldsets();
                $FIELDSET_YOUR_DETAILS.removeClass(FIELDSET_COLLAPSED);
                $EDIT_YOUR_DETAILS.addClass(IS_HIDDEN);
                if ($FIELDSET_PAYMENT_DETAILS.attr(FIELDSET_COMPLETE) !== null) {
                    $EDIT_PAYMENT_DETAILS.removeClass(IS_HIDDEN);
                } else {
                    $EDIT_PAYMENT_DETAILS.addClass(IS_HIDDEN);
                }
            });
        }

        if($EDIT_PAYMENT_DETAILS.length > 0){
            bean.on($EDIT_PAYMENT_DETAILS[0], 'click', function (e) {
                e.preventDefault();
                collapseFieldsets();
                $FIELDSET_PAYMENT_DETAILS.removeClass(FIELDSET_COLLAPSED);
                $EDIT_PAYMENT_DETAILS.addClass(IS_HIDDEN);
                $EDIT_YOUR_DETAILS.removeClass(IS_HIDDEN);
            });
        }
    };

    var collapseFieldsets = function () {
        $FIELDSET_YOUR_DETAILS.addClass(FIELDSET_COLLAPSED);
        $FIELDSET_PAYMENT_DETAILS.addClass(FIELDSET_COLLAPSED);
        $FIELDSET_REVIEW.addClass(FIELDSET_COLLAPSED);
    };

    var reviewDetails = function () {
        if($YOUR_DETAILS_SUBMIT.length > 0){
            bean.on($YOUR_DETAILS_SUBMIT[0], 'click', function (e) {
                e.preventDefault();
                $REVIEW_NAME.text([$FIRST_NAME.val(), $LAST_NAME.val()].join(' '));
                $REVIEW_ADDRESS.text([$ADDRESS1.val(), $ADDRESS2.val(), $ADDRESS3.val(), $POSTCODE.val()].join(', '));
                $REVIEW_EMAIL.text($EMAIL.val());
                $SMALLPRINT.removeClass(IS_HIDDEN);
            });
        }

        if($PAYMENT_DETAILS_SUBMIT.length > 0){
            bean.on($PAYMENT_DETAILS_SUBMIT[0], 'click', function (e) {
                e.preventDefault();
                $REVIEW_ACCOUNT.text($ACCOUNT.val());
                $REVIEW_SORTCODE.text([$SORTCODE1.val(), $SORTCODE2.val(), $SORTCODE3.val()].join('-'));
                $REVIEW_HOLDER.text($HOLDER.val());
            });
        }
    };

    function init() {
        findAddress();
        manualAddress();
        toggleFieldsets();
        reviewDetails();
        emailCheck.init();
    }

    return {
        init: init
    };

});
