/**
 * Append-around pattern
 * Based on https://github.com/filamentgroup/AppendAround
 */
define(['lodash.debounce'], function (debounce) {
    'use strict';

    var APPEND_SELECTOR = '.js-append';
    var SET_SELECTOR = 'data-set';

    function isHidden( elem ){
        return window.getComputedStyle(elem, null).getPropertyValue('display') === 'none';
    }

    function appendToVisibleContainer(el) {
        var parent = el.parentNode;
        var attVal = parent.getAttribute(SET_SELECTOR);
        var attSelector = '[' + SET_SELECTOR + '="' + attVal + '"]';
        var sets = document.querySelectorAll(attSelector);
        if( isHidden( parent ) && sets.length ){
            var found = 0;
            [].forEach.call(sets, function(set) {
                if( !isHidden( set ) && !found ){
                    set.appendChild(el);
                    found++;
                    parent = el;
                }
            });
        }
    }

    function init() {
        var els = document.querySelectorAll(APPEND_SELECTOR);
        if(els.length) {
            [].forEach.call(els, function(el) {
                appendToVisibleContainer(el);
                window.addEventListener('resize', debounce(function() {
                    appendToVisibleContainer(el);
                }), 16);
            });
        }
    }

    return {
        init: init
    };

});
