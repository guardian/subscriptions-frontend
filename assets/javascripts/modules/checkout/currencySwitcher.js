define(['$'], function ($) {
    var currency;

    var setCurrency = function (cur) {
        currency = cur;
        refresh();
    };

    var refresh = function () {
        $('[data-currency]').each(function(el) {
            var $el = $(el);
            if ($el.attr('data-currency') === currency) {
                $el.show();
            } else {
                $el.hide();
            }
        });
    };

    return {
        setCurrency: setCurrency
    }
});
