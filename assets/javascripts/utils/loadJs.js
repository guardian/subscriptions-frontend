define(function () {
    'use strict';

    return function(src, cb, errCb) {
        var ref = window.document.getElementsByTagName('script')[0];
        var script = window.document.createElement('script');

        script.src = src;
        script.async = true;
        ref.parentNode.insertBefore(script, ref);

        if (cb && typeof cb === 'function') {
            script.onload = cb;
        }
        if (errCb && typeof errCb === 'function') {
            script.onerror = errCb;
        }

        return script;
    }
});
