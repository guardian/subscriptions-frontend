define([
    '$',
    'bean',
    'utils/ajax',
    'modules/forms/toggleError',
    'raven'
], function (
    $,
    bean,
    ajax,
    toggleError,
    raven
) {
    'use strict';

    var $FINISH_ACCOUNT_FORM = $('.js-finish-account');
    var $FINISH_ACCOUNT_SUBMIT = $('.js-checkout-finish-account-submit');
    var $FINISH_ACCOUNT_SUCCESS = $('.js-finish-account-success');
    var $FINISH_ACCOUNT_ERROR = $('.js-finish-account-error');
    var $FINISH_ACCOUNT_PASSWORD = $('.js-checkout-finish-account-password .js-input');
    var $FINISH_ACCOUNT_PASSWORD_CONTAINER = $('.js-checkout-finish-account-password');
    var IS_HIDDEN = 'is-hidden';

    function validatePassword(password) {
        /**
         * We have a component which explicitly
         * does password strength validation.
         * Why not use that to determine if we shoule submit??
         * e.g., if zxcvbn says it's not weak?
         */
        return password.length >= 6;
    }

    function init() {
        var $finishAccountForm = $FINISH_ACCOUNT_FORM;
        if ($finishAccountForm.length) {
            $FINISH_ACCOUNT_SUBMIT[0].addEventListener('click', function(e) {
                e.preventDefault();
                var passwordValid = validatePassword($FINISH_ACCOUNT_PASSWORD.val());
                toggleError($FINISH_ACCOUNT_PASSWORD_CONTAINER, !passwordValid);
                if (passwordValid) {
                    ajax({
                        method: 'POST',
                        url: $finishAccountForm.attr('action'),
                        data: ajax.reqwest.serialize($finishAccountForm[0])
                    }).then(function () {
                        $FINISH_ACCOUNT_FORM.addClass(IS_HIDDEN);
                        $FINISH_ACCOUNT_SUCCESS.removeClass(IS_HIDDEN);
                    }).catch(function (err) {
                        $FINISH_ACCOUNT_FORM.addClass(IS_HIDDEN);
                        $FINISH_ACCOUNT_ERROR.removeClass(IS_HIDDEN);
                        raven.Raven.captureException(err);
                    });
                }
            });
        }
    }

    return {
        init: init
    };

});
