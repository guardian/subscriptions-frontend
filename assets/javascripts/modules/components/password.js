/*global zxcvbn */
define(['$', 'bean'], function ($, bean) {
    'use strict';

    var STRENGTH_INDICATOR_ELEM = $('.js-password-strength-indicator')[0];
    var PASSWORD_STRENGTH_INPUT_ELEM = $('.js-password-strength')[0];
    var STRENGTH_LABEL_ELEM = $('.js-password-strength-label')[0];
    var config = {
        text: {
            passwordLabel: 'Password strength',
            errorLong: 'Password too long',
            errorShort: 'Password too short'
        },
        passwordLabels: [
            'weak',
            'poor',
            'medium',
            'good',
            'strong'
        ]
    };

    /**
     * check zxcvbn score and apply relevant className to display strength
     */
    var checkStrength = function() {
        var score = zxcvbn(PASSWORD_STRENGTH_INPUT_ELEM.value).score;
        var label = config.text.passwordLabel + ': ' + config.passwordLabels[score];

        if (PASSWORD_STRENGTH_INPUT_ELEM.value.length < PASSWORD_STRENGTH_INPUT_ELEM.getAttribute('minlength')) {
            label = config.text.errorShort;
            score = null;
        } else if (PASSWORD_STRENGTH_INPUT_ELEM.value.length > PASSWORD_STRENGTH_INPUT_ELEM.getAttribute('maxlength')) {
            label = config.text.errorLong;
            score = null;
        }

        STRENGTH_INDICATOR_ELEM.className = STRENGTH_INDICATOR_ELEM.className.replace(/\bscore-\S+/g, 'score-' + score);
        STRENGTH_LABEL_ELEM.textContent = label;
    };


    /**
     * load in zxcvbn lib as it is ~700kb!
     * setup listener for length check
     * setup listener for zxcvbn.score check
     */
    var addListeners = function () {
        require(['js!zxcvbn'], function() {
            STRENGTH_INDICATOR_ELEM.classList.toggle('is-hidden');

            bean.on(PASSWORD_STRENGTH_INPUT_ELEM, 'keyup', function () {
                checkStrength();
            });
        });
    };

    var init = function() {
        if (PASSWORD_STRENGTH_INPUT_ELEM) {
            addListeners();
        }
    };


    return {
        init: init
    };
});
