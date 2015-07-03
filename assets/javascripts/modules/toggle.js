define(['$', 'bean'], function ($, bean) {
    'use strict';

    var VISIBLE_CLASS = 'visible';

    function init() {
        var elems = $('.js-show-more-button');
        elems.each(function (elem) {
            var $elem = $(elem);
            bean.on(elem, 'click', function () {
                // TODO: abstract this so it doesn't rely on being a button.
                $elem.toggleClass('button--show-more button--show-less');
                $('.js-' + elem.getAttribute('data-toggle-set')).each(function(set){
                    $(set).toggleClass(VISIBLE_CLASS);
                });
            });
        });
    }

    return {
        init: init
    };

});
