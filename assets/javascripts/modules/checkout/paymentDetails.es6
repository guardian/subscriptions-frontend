import bean from 'bean';
import toggleError from 'modules/forms/toggleError';
import formEls from 'modules/checkout/formElements';
import payment from 'modules/checkout/payment';
import eventTracking from 'modules/checkout/eventTracking';
import find from 'lodash/collection/find';
import loader from 'modules/forms/loader';
import assign from 'lodash/object/assign';
var FIELDSET_COMPLETE = 'is-complete';
var FIELDSET_COLLAPSED = 'is-collapsed';
function displayErrors(validity) {
    toggleError(formEls.$ACCOUNT_CONTAINER, !validity.accountNumberValid);
    toggleError(formEls.$HOLDER_CONTAINER, !validity.accountHolderNameValid);
    toggleError(formEls.$SORTCODE_CONTAINER, !validity.sortCodeValid);
    toggleError(formEls.$CONFIRM_PAYMENT_CONTAINER, !validity.detailsConfirmedValid);
}
function nextStep() {
    formEls.$FIELDSET_PAYMENT_DETAILS.addClass(FIELDSET_COLLAPSED).addClass(FIELDSET_COMPLETE);
    formEls.$FIELDSET_REVIEW.removeClass(FIELDSET_COLLAPSED);
    formEls.$FIELDSET_YOUR_DETAILS[0].scrollIntoView();
    eventTracking.completedPaymentDetails();
}
function handleValidation() {
    var paymentMethod = find(formEls.$PAYMENT_METHOD, function (elem) {
        return elem.checked;
    }).value;
    var paymentDetails = { paymentMethod: paymentMethod };
    if (paymentMethod === 'direct-debit') {
        guardian.experience = 'direct-debit';
        assign(paymentDetails, {
            accountNumber: formEls.$ACCOUNT.val(),
            accountHolderName: formEls.$HOLDER.val(),
            sortCode: formEls.$SORTCODE.val(),
            detailsConfirmed: formEls.$CONFIRM_PAYMENT[0].checked
        });
    } else if (paymentMethod === 'card') {
        guardian.experience = 'stripeCheckout';
    } else {
        throw new Error('Invalid payment method ' + paymentMethod);
    }
    loader.setLoaderElem(document.querySelector('.js-payment-details-validating'));
    loader.startLoader();
    payment.validate(paymentDetails).then(function (validity) {
        loader.stopLoader();
        displayErrors(validity);
        if (validity.allValid) {
            nextStep();
        }
    });
}
function init() {
    var $actionEl = formEls.$PAYMENT_DETAILS_SUBMIT;
    if ($actionEl.length) {
        bean.on($actionEl[0], 'click', function (evt) {
            evt.preventDefault();
            handleValidation();
        });
    }
}
export default {
    init: init,
    nextStep: nextStep
};