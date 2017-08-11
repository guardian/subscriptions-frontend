/*global guardian*/
import formElements from 'modules/track/formElements'

export function init() {
    if (formElements.$TRACK_DELIVERY_FORM.length) {
        require(['modules/track/trackDeliveryFields'], function (trackDeliveryFields) {
            trackDeliveryFields.init();
        })
    }
}

