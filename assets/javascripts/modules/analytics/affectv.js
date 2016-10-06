define(['utils/loadJs',
        'modules/analytics/analyticsEnabled',
        'modules/analytics/dntEnabled'
], function(loadJs, analyticsEnabled, dntEnabled) {
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
        var id = urlMapping[window.location.pathname] || false;
        if (id) {
            loadJs(scriptUrl + id);
        }
    }

    return {
        init: analyticsEnabled(dntEnabled(init))
    };
});
