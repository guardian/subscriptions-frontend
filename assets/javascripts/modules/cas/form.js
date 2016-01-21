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
    var $CONTAINER = $('#cas-tool');
    var $SUBSCRIPTION_FORM = $('#cas-subscription-form');
    var $GENERATE_TOKEN_FORM = $('#cas-generate-token-form');
    var $RESULTS = $('dl[data-cas-result]');
    var $TOKEN_RESULT = $('[data-cas-token]', $CONTAINER);
    var $ERROR = $('[data-cas-error]', $CONTAINER);

    function submitSubscription() {
        var data = serializer([].slice.call($SUBSCRIPTION_FORM[0].elements));
        console.log('submitting data', data);
        ajax({
            url: $SUBSCRIPTION_FORM[0].action,
            method: 'post',
            data: data,
            success: function(successData) {
                console.log(successData);
                redrawSubscription(successData);
                $ERROR.hide();
            },
            error: function(err) {
                var message = JSON.parse(err.response).message;
                console.error('Some exception occurred', err);
                redrawSubscription({});
                $ERROR.text('Failed finding the user: ' + message);
                $ERROR.show();
            }
        });
    }

    function redrawSubscription(model) {
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

    function submitGenerateToken() {
        var data = serializer([].slice.call($GENERATE_TOKEN_FORM[0].elements));
        console.log('submitting data', data);
        ajax({
            url: $GENERATE_TOKEN_FORM[0].action,
            method: 'post',
            data: data,
            success: function(successData) {
                console.log(successData);
                redrawGenerateToken(successData.token);
                $ERROR.hide();
            },
            error: function() {
                $ERROR.text('Invalid data sent, please ask the Membership team');
                $ERROR.show();
            }
        });
    }

    function redrawGenerateToken(token) {
        if (token) {
            $TOKEN_RESULT.text(token);
            $TOKEN_RESULT.show();
        } else {
            $TOKEN_RESULT.hide();
        }
    }


    function init() {
        if ($CONTAINER.length) {
            bean.on($SUBSCRIPTION_FORM[0], 'submit', function (e) {
                e.preventDefault();
                submitSubscription();
            });
            redrawSubscription({});

            bean.on($GENERATE_TOKEN_FORM[0], 'submit', function (e) {
                e.preventDefault();
                submitGenerateToken();
            });
        }
    }

    return {
        init: init
    };
});
