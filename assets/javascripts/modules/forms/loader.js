define(function() {
    'use strict';

    var LOADER_ELEM = document.querySelector('.js-loader');
    var LOADING_CLASS = 'is-loading';

    var startLoader = function () {
        LOADER_ELEM.classList.add(LOADING_CLASS);
    };

    var stopLoader = function () {
        LOADER_ELEM.classList.remove(LOADING_CLASS);
    };

    var setProcessingMessage = function (msg) {
        LOADER_ELEM.textContent = msg;
    };

    return {
        setProcessingMessage: setProcessingMessage,
        startLoader: startLoader,
        stopLoader: stopLoader
    };
});
