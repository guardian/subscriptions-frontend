define([], function () {

    function setPaymentToken(token) {
        var field = document.querySelector('.js-payment-token');
        field.value = token
    }

    return ({
        setPaymentToken: setPaymentToken
    })
});
