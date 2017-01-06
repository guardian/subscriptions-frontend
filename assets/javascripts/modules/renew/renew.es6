import ajax from 'ajax';
import {check} from '../promoCode';

let stripeHandler = null;

export class SortCode {
    constructor(sortCode) {
        this.value = this.formatSortCode(sortCode);
        this.isValid = this.value.length === 8;
    }

    formatSortCode(s) {
        const split = /\d{1,2}/g;
        const strip = /\D/g;
        let array = s.replace(strip, '').match(split); //This returns false if there aren't any numbers in the string
        return array ? array.slice(0, 3).join('-') : ''
    }


}
let renewErrorMessage = 'Sorry, your subscription could not be renewed. Please contact us at gwsubs@theguardian.com or call +44 (0) 330 333 6767. Lines open weekdays 8am-8pm, weekend 8pm-6pm';

export function validAccount(accountNumber) {
    return /^\d{6,10}$/.test(accountNumber);
}

export function validEmail(email) {
    return /.\@./.test(email);
}

export const STRIPE = "STRIPE";
export const DIRECT_DEBIT = "DIRECTDEBIT";

export function validState(state) {
    if (!state.email.isValid) {
        return false;
    }
    if (state.paymentType === STRIPE) {
        return true;
    }
    return state.sortCode.isValid
        && state.accountNumber.isValid
        && state.accountHolder.isValid
        && state.directDebitConfirmed.isValid;
}

export function send(state, errorHandler) {
    let stripePayload = (token) => {
        return {
            type: 'card',
            stripeToken: token
        };
    };
    let tokenHandler = (token) => {
        post(stripePayload(token.id));
    };
    let getStripe = () => {
        stripeHandler.open({
            email: state.email.value,
            token: tokenHandler
        });
    };
    let directDebitPayload = () => {
        return {
            type: 'direct-debit',
            sortCodeValue: state.sortCode.value,
            account: state.accountNumber.value,
            holder: state.accountHolder.value,
        }
    };

    let post = (paymentData) => {
        let payload = {
            email: state.email.value,
            plan: state.plan,
            paymentData: paymentData
        };
        let promoCode = state.promoCode;
        if(promoCode){
            payload.promoCode = promoCode;
        }
            ajax({
            type: 'json',
            method: 'POST',
            url: '/manage/renew',
            headers: {
                'Csrf-Token': document.querySelector('input[name="csrfToken"]').getAttribute('value')
            },
            contentType: 'application/json',
            data: JSON.stringify(payload)
        }).then((response) => {
            window.location.assign(response.redirect);
        }).catch((r) => {
            errorHandler(renewErrorMessage);
        });
    };
    if (state.paymentType === STRIPE) {
        getStripe();
    } else {
        post(directDebitPayload());
    }
}

export function init() {
    stripeHandler = window.StripeCheckout.configure(guardian.stripeCheckout);
}

