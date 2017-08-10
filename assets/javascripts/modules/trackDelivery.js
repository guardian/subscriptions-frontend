/*global guardian*/
define([
    'modules/track/formElements'
], function (
    formElements
) {
    'use strict';
    function init() {
        if (formElements.$TRACK_DELIVERY_FORM.length) {
            require(['modules/track/trackDeliveryFields'], function(trackDeliveryFields) {
                trackDeliveryFields.default.init();
            });
        }
    }

    return {
        init: init
    };


});
