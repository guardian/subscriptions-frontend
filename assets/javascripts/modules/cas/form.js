define([
    '$',
    'bean',
    'utils/ajax',
    'utils/serializer'
], function (
    $,
    bean,
    ajax,
    serializer
) {
    var $FORM = $('#cas-form');
    var $RESULTS = $('dl[data-cas-result]');
    var $ERROR = $('[data-cas-error]');

    function submit() {
        var data = serializer([].slice.call($FORM[0].elements));
        console.log('submitting data', data);
        ajax({
            url: $FORM[0].action,
            method: 'post',
            data: data,
            success: function(successData) {
                console.log(successData);
                redraw(successData);
                $ERROR.hide();
            },
            error: function(err) {
                var message = JSON.parse(err.response).message;
                console.error('Some exception occurred', err);
                redraw({});
                $ERROR.text('Failed finding the user: ' + message);
                $ERROR.show();
            }
        });
    }

    function redraw(model) {
        $RESULTS.each(function(el) {
            var field = $(el).attr('data-cas-result');

            if (model[field]) {
                $('dd', $(el)).text(model[field]);
                $(el).show();
            } else {
                $(el).hide();
            }
        });
    }

    function init() {
        if ($FORM.length) {
            console.log('cas form');
            bean.on($FORM[0], 'submit', function (e) {
                e.preventDefault();
                submit();
            });
            redraw({});
        }
    }

    return {
        init: init
    };
});
