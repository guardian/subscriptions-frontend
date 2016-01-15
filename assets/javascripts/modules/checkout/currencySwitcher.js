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
                $('input[type="radio"]', $el).each(function(input) {
                    $(input).removeAttr('disabled');
                });
            } else {
                $el.hide();
                $('input[type="radio"]', $el).each(function(input) {
                    $(input).removeAttr('checked');
                    $(input).attr('disabled', 'disabled');
                });
            }
        });

    };

    return {
        setCurrency: setCurrency
    }
});
