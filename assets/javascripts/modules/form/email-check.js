define(['bean',
    'utils/ajax',
    'utils/text',
    'utils/cookie'
], function (bean, ajax, textUtils, cookie) {

    'use strict';

    function init() {

        // do nothing if the user is already signed in
        if(cookie.getCookie('GU_U')) { return; }

        var YOUR_DETAILS_SUBMIT_ELEM = document.querySelector('.js-checkout-your-details-submit');
        var EMAIL_ID_INPUT_ELEM = document.querySelector('.js-checkout-email');

        ajax.init({page: {ajaxUrl: ''}});

        bean.on(YOUR_DETAILS_SUBMIT_ELEM, 'click', function (event) {
            if (EMAIL_ID_INPUT_ELEM) {
                var email = textUtils.removeWhitespace(EMAIL_ID_INPUT_ELEM.value);

                if (email.length) {
                    ajax({
                        url: '/checkout/check-identity?email=' + email
                    }).then(function (response) {
                        if (!response.emailInUse) {
                            bean.fire(YOUR_DETAILS_SUBMIT_ELEM, "submit");
                        } else {
                            alert("Your email is already in use! Please sign in or use another email address");
                        }
                    }).fail(function (_, msg) {
                        console.error("Error reaching endpoint /checkout/check-identity:", msg);
                    });
                } else {
                    alert("Please enter an email address");
                }
            }
        });
    }

    return {
        init: init
    };
});
