define(['bean'], function (bean) {

    var IS_HIDDEN = 'is-hidden',
        MANUAL_ADDRESS_ELEM = document.querySelector('.js-manual-address'),
        FULL_ADDRESS_ELEM = document.querySelector('.js-full-address');

    var manualAddress = function() {
        bean.on(MANUAL_ADDRESS_ELEM, 'click', function (e) {
            e.preventDefault();
            e.stopImmediatePropagation();
            FULL_ADDRESS_ELEM.classList.remove(IS_HIDDEN);
            MANUAL_ADDRESS_ELEM.classList.add(IS_HIDDEN);
        });
    };

    function init() {
        manualAddress();
    }

    return {
        init: init
    };

});
