define(['$', 'bonzo', 'bower_components/bean/bean'], function ($, bonzo, bean) {

    function init() {
        $('.js-show-more-button').each(function (btn) {
            bean.on(btn, 'click', function (e) {
                bonzo(btn).toggleClass('button--show-more button--show-less');
                $('.js-' + bonzo(btn).attr('data-toggle-set')).each(function(elem){
                    bonzo(elem).toggleClass('visible');
                });
            });
        })
    }

    return {
        init: init
    };

});
