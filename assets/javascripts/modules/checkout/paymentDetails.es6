import bean from 'bean';
import toggleError from 'modules/forms/toggleError';
import formEls from 'modules/checkout/formElements';
import payment from 'modules/checkout/payment';
import eventTracking from 'modules/checkout/eventTracking';
import loader from 'modules/forms/loader';
var FIELDSET_COMPLETE = 'is-complete';
var FIELDSET_COLLAPSED = 'is-collapsed';
function displayErrors(validity) {
    toggleError(formEls.$ACCOUNT_CONTAINER, !validity.accountNumberValid);
    toggleError(formEls.$HOLDER_CONTAINER, !validity.accountHolderNameValid);
    toggleError(formEls.$SORTCODE_CONTAINER, !validity.sortCodeValid);
    toggleError(formEls.$CONFIRM_PAYMENT_CONTAINER, !validity.detailsConfirmedValid);
}
export function nextStep() {
    formEls.$FIELDSET_PAYMENT_DETAILS.addClass(FIELDSET_COLLAPSED).addClass(FIELDSET_COMPLETE);
    formEls.$FIELDSET_REVIEW.removeClass(FIELDSET_COLLAPSED);
    formEls.$FIELDSET_YOUR_DETAILS[0].scrollIntoView();
    eventTracking.completedPaymentDetails();
}

function getPaymentDetails() {
    let checkedMethod = document.querySelectorAll(`${formEls.PAYMENT_METHOD}:checked`)

    if (checkedMethod.length !== 1) {
        throw new Error('No payment method selected.');
    }

    let method = checkedMethod[0].value

    if (method === 'direct-debit') {
        guardian.experience = 'direct-debit';
        return {
            paymentMethod: method,
            accountNumber: formEls.$ACCOUNT.val(),
            accountHolderName: formEls.$HOLDER.val(),
            sortCode: formEls.$SORTCODE.val(),
            detailsConfirmed: formEls.$CONFIRM_PAYMENT[0].checked
        }
    } else if (method === 'card') {
        guardian.experience = 'stripeCheckout';
        return { paymentMethod: method };

    }
    throw new Error('Invalid payment method ' + method);
}

function handleValidation() {
    let paymentDetails = getPaymentDetails();
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

export function init() {
    let $actionEl = formEls.$PAYMENT_DETAILS_SUBMIT;
    if ($actionEl.length) {
        bean.on($actionEl[0], 'click', function (evt) {
            evt.preventDefault();
            handleValidation();
        });
    }
}
