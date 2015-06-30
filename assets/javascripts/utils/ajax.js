define([
    'reqwest'
], function (
    reqwest
) {
    'use strict';

    var makeAbsolute = function () {
        throw new Error('AJAX has not been initialised yet');
    };

    function ajax(params) {
        if (!params.url.match('^https?://')) {
            params.url = makeAbsolute(params.url);
        }
        return ajax.reqwest(params);
    }

    ajax.reqwest = reqwest; // expose publicly so we can inspect it in unit tests

    ajax.init = function (config) {
        makeAbsolute = function (url) {
            return config.page.ajaxUrl + url;
        };
    };

    return ajax;

});
