define(function () {
    'use strict';

    var paymentErrMsgs = {
        invalid_request_error: {},
        api_error: {
            rate_limit: ''
        },
        card_error: {
            incorrect_number: {
msg: 'Sorry, the card number that you have entered is incorrect. Please check and retype.'
            },
            incorrect_cvc: {
                msg: 'Sorry, the security code that you have entered is incorrect. Please check and retype.'
            },
            invalid_number: {
msg: 'Sorry, the card number that you have entered is incorrect. Please check and retype.'
            },
            invalid_expiry: {
                msg: 'Sorry, the expiry date that you have entered is invalid. Please check and re-enter.'
            },
            invalid_expiry_month: {
                msg: 'Sorry, the expiry date that you have entered is invalid. Please check and re-enter.'
            },
            invalid_expiry_year: {
                msg: 'Sorry, the expiry date that you have entered is invalid. Please check and re-enter.'
            },
            invalid_cvc: {
                msg: 'Sorry, the security code that you have entered is invalid. Please check and retype.'
            },
            expired_card: {
msg: 'Sorry, this card has expired. Please try again with another card.'
            },
            card_declined: {
generic_decline: 'We\'re sorry. Your card has been declined.',
                card_not_supported: 'We\'re sorry. We can\'t take payment with this type of card. Please try again using Visa, Mastercard or American Express.',
                try_again_later: 'We can\'t process your payment right now. Please try again later.'
            },
            processing_error: {
msg: 'Sorry, we weren\'t able to process your payment this time around. Please try again.'
            },
            client_validation: {
msg: 'Sorry, we\'ve found some problems with your details. Please check and retype.'
            }
        },
        PaymentGatewayError: { // errors due to card's issuing bank rejecting the transactions for exact reasons only known to the bank
            Fraudulent: {
msg: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.'
            },
            TransactionNotAllowed: {
msg: 'Sorry we could not take your payment because your card does not support this type of purchase. Please try a different card or contact your bank to find the cause.'
            },
            DoNotHonor: {
msg: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.'
            },
            InsufficientFunds: {
msg: 'Sorry we could not take your payment because your bank indicates insufficient funds. Please try a different card or contact your bank to find the cause.'
            },
            RevocationOfAuthorization: {
msg: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.'
            },
            GenericDecline: {
msg: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.'
            },
            UnknownPaymentError: {
msg: 'Sorry we could not take your payment. Please try a different card or contact your bank to find the cause.'
            }
        },
        generic_error: {
            msg: 'Sorry, we weren\'t able to process your payment this time around. Please try again.'
        }
    };

    var getMessage = function (err) {
        var errCode = err && err.code;
        var errType = err && err.type;
        var errSection = paymentErrMsgs[errType];
        var errMsg;

        if (errSection) {
            errMsg = errSection[errCode].msg;

            if (errCode === 'card_declined') {
                errMsg = errSection.card_declined[err.decline_code];
                if (!errMsg) {
                    errMsg = errSection.card_declined.generic_decline;
                }
            }
        }

        return errMsg;
    };

    return {
        getMessage: getMessage,
        default:  paymentErrMsgs.generic_error.msg
    };
});
