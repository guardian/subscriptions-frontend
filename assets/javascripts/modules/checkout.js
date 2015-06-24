define([
    '$',
    'bean',
    'modules/checkout/form-elements',
    'modules/checkout/email-check'
], function ($, bean, form, emailCheck) {

    var $FIRST_NAME                 = form.$FIRST_NAME,
        $LAST_NAME                  = form.$LAST_NAME,
        $EMAIL                      = form.$EMAIL,
        $ADDRESS1                   = form.$ADDRESS1,
        $ADDRESS2                   = form.$ADDRESS2,
        $ADDRESS3                   = form.$ADDRESS3,
        $POSTCODE                   = form.$POSTCODE,
        $ACCOUNT                    = form.$ACCOUNT,
        $SORTCODE1                  = form.$SORTCODE1,
        $SORTCODE2                  = form.$SORTCODE2,
        $SORTCODE3                  = form.$SORTCODE3,
        $HOLDER                     = form.$HOLDER,
        $FIND_ADDRESS               = form.$FIND_ADDRESS,
        $MANUAL_ADDRESS             = form.$MANUAL_ADDRESS,
        $FULL_ADDRESS               = form.$FULL_ADDRESS,
        $YOUR_DETAILS_SUBMIT        = form.$YOUR_DETAILS_SUBMIT,
        $PAYMENT_DETAILS_SUBMIT     = form.$PAYMENT_DETAILS_SUBMIT,
        $REVIEW_NAME                = form.$REVIEW_NAME,
        $REVIEW_ADDRESS             = form.$REVIEW_ADDRESS,
        $REVIEW_EMAIL               = form.$REVIEW_EMAIL,
        $REVIEW_ACCOUNT             = form.$REVIEW_ACCOUNT,
        $REVIEW_SORTCODE            = form.$REVIEW_SORTCODE,
        $REVIEW_HOLDER              = form.$REVIEW_HOLDER,
        $SMALLPRINT                 = form.$SMALLPRINT,
        $FIELDSET_YOUR_DETAILS      = form.$FIELDSET_YOUR_DETAILS,
        $FIELDSET_PAYMENT_DETAILS   = form.$FIELDSET_PAYMENT_DETAILS,
        $FIELDSET_REVIEW            = form.$FIELDSET_REVIEW,
        $EDIT_YOUR_DETAILS          = form.$EDIT_YOUR_DETAILS,
        $EDIT_PAYMENT_DETAILS       = form.$EDIT_PAYMENT_DETAILS,
        FIELDSET_COLLAPSED          = 'fieldset--collapsed',
        FIELDSET_COMPLETE           = 'data-fieldset-complete',
        IS_HIDDEN                   = 'is-hidden';

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
                emailCheck.warnIfEmailTaken().then(function () {
                    $FIELDSET_YOUR_DETAILS.addClass(FIELDSET_COLLAPSED).attr(FIELDSET_COMPLETE, '');
                    $FIELDSET_PAYMENT_DETAILS.removeClass(FIELDSET_COLLAPSED);
                    $EDIT_YOUR_DETAILS.removeClass(IS_HIDDEN);
                    $EDIT_PAYMENT_DETAILS.addClass(IS_HIDDEN);
                }).catch(function (e) {
                    console.error("failed email check:", e);
                });
            })
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
        emailCheck.init();
        findAddress();
        manualAddress();
        toggleFieldsets();
        reviewDetails();
    }

    return {
        init: init
    };

});
