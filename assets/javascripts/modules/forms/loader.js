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

    var setLoaderElem = function(elem) {
        LOADER_ELEM = elem;
    };

    return {
        setLoaderElem: setLoaderElem,
        setProcessingMessage: setProcessingMessage,
        startLoader: startLoader,
        stopLoader: stopLoader
    };
});
