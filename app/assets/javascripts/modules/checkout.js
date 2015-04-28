define(['$', 'bean'], function ($, bean) {

    var $FIRST_NAME             = $('.js-checkout-first'),
        $LAST_NAME              = $('.js-checkout-last'),
        $EMAIL                  = $('.js-checkout-email'),
        $ADDRESS1               = $('.js-checkout-house'),
        $ADDRESS2               = $('.js-checkout-street'),
        $ADDRESS3               = $('.js-checkout-town'),
        $POSTCODE               = $('.js-checkout-postcode'),
        $ACCOUNT                = $('.js-checkout-account'),
        $SORTCODE1              = $('.js-checkout-sortcode1'),
        $SORTCODE2              = $('.js-checkout-sortcode2'),
        $SORTCODE3              = $('.js-checkout-sortcode3'),
        $HOLDER                 = $('.js-checkout-holder'),
        $FIND_ADDRESS           = $('.js-checkout-find-address'),
        $MANUAL_ADDRESS         = $('.js-checkout-manual-address'),
        $FULL_ADDRESS           = $('.js-checkout-full-address'),
        $YOUR_DETAILS_SUBMIT    = $('.js-checkout-your-details-submit'),
        $PAYMENT_DETAILS_SUBMIT = $('.js-checkout-payment-details-submit'),
        $REVIEW_NAME            = $('.js-checkout-review-name'),
        $REVIEW_ADDRESS         = $('.js-checkout-review-address'),
        $REVIEW_EMAIL           = $('.js-checkout-review-email'),
        $REVIEW_ACCOUNT         = $('.js-checkout-review-account'),
        $REVIEW_SORTCODE        = $('.js-checkout-review-sortcode'),
        $REVIEW_HOLDER          = $('.js-checkout-review-holder'),
        $SMALLPRINT             = $('.js-checkout-smallprint'),

        IS_HIDDEN = 'is-hidden';

    var findAddress = function () {
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
    };

    var populateAddressFields = function (address) {
        $ADDRESS1.val(address.house);
        $ADDRESS2.val(address.street);
        $ADDRESS3.val(address.town);
    };

    var manualAddress = function () {
        bean.on($MANUAL_ADDRESS[0], 'click', function (e) {
            e.preventDefault();
            showFullAddressFields();
            $MANUAL_ADDRESS.addClass(IS_HIDDEN);
        });
    };

    var showFullAddressFields = function () {
        $FULL_ADDRESS.removeClass(IS_HIDDEN);
    };

    var reviewDetails = function () {
        bean.on($YOUR_DETAILS_SUBMIT[0], 'click', function (e) {
            e.preventDefault();
            $REVIEW_NAME.text([$FIRST_NAME.val(), $LAST_NAME.val()].join(' '));
            $REVIEW_ADDRESS.text([$ADDRESS1.val(), $ADDRESS2.val(), $ADDRESS3.val(), $POSTCODE.val()].join(', '));
            $REVIEW_EMAIL.text($EMAIL.val());
            $SMALLPRINT.removeClass(IS_HIDDEN);
        });

        bean.on($PAYMENT_DETAILS_SUBMIT[0], 'click', function (e) {
            e.preventDefault();
            $REVIEW_ACCOUNT.text($ACCOUNT.val());
            $REVIEW_SORTCODE.text([$SORTCODE1.val(), $SORTCODE2.val(), $SORTCODE3.val()].join('-'));
            $REVIEW_HOLDER.text($HOLDER.val());
        });
    };

    function init() {
        findAddress();
        manualAddress();
        reviewDetails();
    }

    return {
        init: init
    };

});
