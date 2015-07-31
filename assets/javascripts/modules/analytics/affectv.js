/*global Raven */
define(function() {
    'use strict';

    var digitalPackId = '546dd61001896028ffd29273';
    var urlMapping = {
        '/': '54e4b48a0189603f6b557597',
        '/digital': digitalPackId,
        '/us/digital': digitalPackId,
        '/au/digital': digitalPackId
    };

    function init() {
        var scriptUrl = 'https://go.affec.tv/j/';
        // Specific page tracking if we match a given path
        var id = urlMapping[window.location.pathname] || false;
        if(id) {
            scriptUrl = scriptUrl + id + '?noext';
            require('js!' + scriptUrl).then(null, function(err) {
                Raven.captureException(err);
            });
        }
    }

    return {
        init: init
    };
});
