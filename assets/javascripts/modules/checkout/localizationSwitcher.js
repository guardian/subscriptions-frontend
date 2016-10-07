define(['$'], function ($) {
    'use strict';

    var model = {
       currency: null,
       country: null
    };
//TODO WE NEED TO SEE IF THE RIGHT ADDRESS IS BEING MODIFIED (DELIVERY FOR WEEKLY, BILLING FOR DIGIPACK)
    var refresh = function () {
        ['currency', 'country'].forEach(function (param) {
            var dataAttr = 'data-' + param;
            $('[' + dataAttr + ']').each(function(el) {
                var $el = $(el);
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
    //TODO check if there is a way or reusing the code above or do something more general than this
    updateHiddenCurrency();
    };

    var updateHiddenCurrency = function () {
        $('[data-hidden-currency]').each(function (el) {
            var $el = $(el);
            $el.val(model['currency']);
        });
    }
    var set = function (currency, country) {
        model.currency = currency;
        model.country = country;
        refresh();
    };

    return {
        set: set
    };
});
