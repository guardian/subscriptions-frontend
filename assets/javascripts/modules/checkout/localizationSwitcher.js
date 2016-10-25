define(['$'], function ($) {
    'use strict';
//TODO THIS IS A GENERAL PURPOSE SWITCHER NOW SINCE YOU CAN PASS ANY OBJECT WITH VALUES AND IT WILL UPDATE THEM, IT NEVER ASSUMES THAT IT IS RELATED TO LOCATION

    var refresh = function (model) {
        Object.keys(model).forEach(function (param) {
            var dataAttr = 'data-' + param;
            $('[' + dataAttr + ']').each(function(el) {
                var $el = $(el);
                if ($el.attr('type') == 'hidden'){
                    $el.val(model[param]);
                } else
                if ($el.attr(dataAttr) === model[param]) {
                    $el.show();
                } else {
                    $el.hide();
                    $('input[type="radio"]', $el).each(function(input) {
                        $(input).removeAttr('checked');
                    });
                }
            });
        });

    };

    return {
        refresh: refresh
    };
});
