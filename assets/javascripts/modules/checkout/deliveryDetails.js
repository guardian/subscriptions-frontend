define([
    'modules/forms/checkFields',
    'modules/checkout/formElements',
    'bean'
], function (
    checkFields,
    formEls,
    bean) {
    'use strict';

    var FIELDSET_COMPLETE = 'is-complete';
    var FIELDSET_COLLAPSED = 'is-collapsed';


    return {
        init: function() {

            if (!formEls.DELIVERY.$CONTAINER.length) {
                return;
            }
            
            bean.on(formEls.$DELIVERY_DETAILS_SUBMIT[0], 'click', function(e) {

                e.preventDefault();
                if (!checkFields.checkRequiredFields(formEls.DELIVERY.$CONTAINER)) {
                    return;
                }

                formEls.$FIELDSET_PAYMENT_DETAILS.removeClass(FIELDSET_COLLAPSED);

                formEls.$FIELDSET_DELIVERY_DETAILS
                    .addClass(FIELDSET_COLLAPSED)
                    .addClass(FIELDSET_COMPLETE)[0]
                    .scrollIntoView();

                formEls.$NOTICES.removeAttr('hidden');
            });
        }
    };
});
