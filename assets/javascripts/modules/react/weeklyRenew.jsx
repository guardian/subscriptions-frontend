import React from 'react'
import ReactDOM from 'react-dom';
import { RadioGroup, RadioButton as UnspacedRadioButton } from 'react-radio-buttons';

import { DirectDebit } from 'modules/react/directDebit'
import { PromoCode, status } from 'modules/react/promoCode'
import { validatePromoCode, combinePromotionAndPlans } from '../promoCode';
import {
    SortCode,
    validAccount,
    validEmail,
    validState,
    send,
    STRIPE,
    DIRECT_DEBIT,
    init as stripeInit
} from 'modules/renew/renew'

const RadioButton = ({ children, ...props }) => <div style={{ boxSizing: 'unset' }}><UnspacedRadioButton pointColor="#005689" {...props}><span style={{ paddingRight: '10px' }}>{children}</span></UnspacedRadioButton></div>;
//This component sets styles directly, and they conflict a bit with our css.

const empty = {
    value: '',
    isValid: false
};
export function init(container) {
    stripeInit();
    let showPaymentType = container.dataset.billingCountry === 'GB' && container.dataset.currency === 'GBP';
    let plans = window.guardian.plans;
    ReactDOM.render(<WeeklyRenew
        showPaymentType={showPaymentType}
        email={container.dataset.email}
        currency={container.dataset.currency}
        country={container.dataset.deliveryCountry}
        plans={plans}
        promoCode={container.dataset.promoCode}
        directDebitLogo={container.dataset.directDebitLogo}
        weeklyTermsConditionsHref={container.dataset.weeklyTermsConditionsHref}
        weeklyTermsConditionsTitle={container.dataset.weeklyTermsConditionsTitle}
        privacyPolicyHref={container.dataset.privacyPolicyHref}
        privacyPolicyTitle={container.dataset.privacyPolicyTitle}
        whyYourDataMattersToUsHref={container.dataset.whyYourDataMattersToUsHref}
    />,
        container);
}

class WeeklyRenew extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            error: false,
            email: props.email ? { value: props.email, isValid: true } : empty,
            paymentType: this.props.showPaymentType ? DIRECT_DEBIT : STRIPE,
            sortCode: empty,
            accountNumber: empty,
            accountHolder: empty,
            directDebitConfirmed: empty,
            loading: false,
            showValidity: false,
            plan: this.props.plans[0].id,
            plans: this.props.plans,
            displayedPrice: this.props.plans[0].price,
            promoCode: this.props.promoCode,
            promoStatus: status.NOTCHECKED,
            promotionDescription: null
        };
        this.showEmail = !this.state.email.isValid;
        this.handlePromo = this.handlePromo.bind(this);
        this.handleError = this.handleError.bind(this);
        this.handleEmail = this.handleEmail.bind(this);
        this.handlePaymentType = this.handlePaymentType.bind(this);
        this.handleSortCode = this.handleSortCode.bind(this);
        this.handleAccountNumber = this.handleAccountNumber.bind(this);
        this.handleAccountHolder = this.handleAccountHolder.bind(this);
        this.handleDirectDebitConfirmation = this.handleDirectDebitConfirmation.bind(this);
        this.click = this.click.bind(this);
        this.buttonText = this.buttonText.bind(this);
        this.validatePromo = this.validatePromo.bind(this);
        this.getPrice = this.getPrice.bind(this);
        this.handlePlan = this.handlePlan.bind(this);
        this.getPlan = this.getPlan.bind(this);

        if (this.props.promoCode !== '') {
            this.validatePromo();
        }
    }


    click() {
        this.setState({ showValidity: true });
        if (validState(this.state)) {
            this.setState({ loading: true });
            send(this.state, this.handleError);
        }
    }

    handlePromo(e) {
        this.setState({
            promoCode: e.target.value.trim(),
            promoStatus: status.NOTCHECKED,
            plans: this.props.plans
        });

    };

    validatePromo() {
        this.setState({
            promosStatus: status.LOADING
        });

        validatePromoCode(this.state.promoCode, this.props.country, this.props.currency).then((response) => {
            let newPlans = combinePromotionAndPlans(response, this.state.plans);
            let valid = response.promotion.promotionType.name === 'retention' || (response.promotion.promotionType.name === 'double' && (response.promotion.promotionType.a.name === 'retention' || response.promotion.promotionType.b.name === 'retention'));
            if (valid) {
                let price = this.getPrice(this.getPlan(newPlans));
                //This is a weekly promotion
                this.setState({
                    promoStatus: status.VALID,
                    plans: newPlans,
                    promotionDescription: response.promotion.description,
                    displayedPrice: price
                });
            } else {
                let price = this.getPrice(this.getPlan(this.props.plans));
                //It's a good promotion, but it's not a weekly one
                this.setState({
                    promotionDescription: 'The promotion you have entered is not currently valid to renew your subscription.',
                    promoStatus: status.INVALID,
                    plans: this.props.plans,
                    displayedPrice: price
                });
            }
        }).catch((payload) => {
            let price = this.getPrice(this.getPlan(this.props.plans));
            let error = "Invalid Promotion";
            if ("response" in payload) {
                try {
                    error = JSON.parse(payload.response).errorMessage;
                } catch (e) {
                    console.error(e);
                }
            }
            this.setState({
                promotionDescription: error,
                promoStatus: status.INVALID,
                plans: this.props.plans,
                displayedPrice: price
            })
        });
    }

    handleEmail(e) {
        let email = e.target.value;
        this.setState({ email: { value: email, isValid: validEmail(email) } });
    };

    handlePaymentType(value) {
        this.setState({ paymentType: value })
    };

    handleSortCode(e) {
        this.setState({ sortCode: new SortCode(e.target.value) })
    }

    handleAccountNumber(e) {
        let accountNumber = e.target.value;
        this.setState({ accountNumber: { value: accountNumber, isValid: validAccount(accountNumber) } });
    }

    handleAccountHolder(e) {
        let name = e.target.value;
        this.setState({ accountHolder: { value: name, isValid: name.length > 0 } });
    }

    handleDirectDebitConfirmation(e) {
        let checked = e.target.checked;
        this.setState({ directDebitConfirmed: { value: checked, isValid: checked === true } })
    }

    handlePlan(id) {
        this.setState({ plan: id, displayedPrice: this.getPrice(id) });
    }

    getPrice(id) {
        let plan = this.state.plans.find(p => p.id == id)
        return plan.promotionalPrice || plan.price;
    }

    getPlan(plans) {
        if (plans == null) {
            plans = this.state.plans;
        }
        return plans.filter((plan) => {
            return plan.id == this.state.plan
        })[0]
    }

    handleError(error) {
        this.setState({ error: error, loading: false });
    }

    buttonText() {
        let method = this.state.paymentType === DIRECT_DEBIT ? 'by Direct Debit' : 'by Card';
        return ['Pay', this.state.displayedPrice, method].join(' ');
    }

    render() {
        return <div>
            {this.state.loading && <div className="loader is-loading"> Processing</div>}
            {this.state.error && <div className="mma-error"> {this.state.error}</div>}
            {!this.state.loading && !this.state.error &&
                <div>

                    <dl className="mma-section__list">

                        <PromoCode
                            value={this.state.promoCode}
                            handler={this.handlePromo}
                            send={this.validatePromo}
                            status={this.state.promoStatus}
                            copy={this.state.promotionDescription}
                        />
                        <PlanChooser plans={this.state.plans} selected={this.getPlan()} handleChange={this.handlePlan} />
                        {this.showEmail &&
                            <EmailField value={this.state.email.value}
                                valid={!this.state.showValidity || this.state.email.isValid}
                                handleChange={this.handleEmail} />}
                        <Payment
                            showPaymentType={this.props.showPaymentType}
                            handlePaymentType={this.handlePaymentType}
                            paymentType={this.state.paymentType}
                            handleSortCode={this.handleSortCode}
                            handleAccountNumber={this.handleAccountNumber}
                            handleAccountHolder={this.handleAccountHolder}
                            handleDirectDebitConfirmation={this.handleDirectDebitConfirmation}
                            sortCode={this.state.sortCode.value}
                            accountNumber={this.state.accountNumber.value}
                            accountHolder={this.state.accountHolder.value}
                            directDebitConfirmed={this.state.directDebitConfirmed.value}
                            validSortCode={!this.state.showValidity || this.state.sortCode.isValid}
                            validAccountNumber={!this.state.showValidity || this.state.accountNumber.isValid}
                            validAccountHolder={!this.state.showValidity || this.state.accountHolder.isValid}
                            validDirectDebitConfirmed={!this.state.showValidity || this.state.directDebitConfirmed.isValid}
                            directDebitLogo={this.props.directDebitLogo}
                        />
                    </dl>
                    <dl className="mma-section__list">
                        <dt className="mma-section__list--title">Review and confirm</dt>
                        <dd className="mma-section__list--content">
                            <div className="u-note prose">
                                <p>
                                    Our <a href={this.props.privacyPolicyHref} target="_blank">{this.props.privacyPolicyTitle}</a> explains in further detail how we use your information and you can find out why your data matters to us <a href={this.props.whyYourDataMattersToUsHref} target="_blank">here</a>.
                            </p>
                                <p>
                                    By proceeding you agree to the <a href={this.props.weeklyTermsConditionsHref} target="_blank">{this.props.weeklyTermsConditionsTitle}</a> for the Guardian Weekly print subscription services.
                            </p>
                            </div>
                            <button className="button button--primary" onClick={this.click}>{this.buttonText()}</button>
                        </dd>
                    </dl>
                </div>
            }

        </div>
    }
}

class EmailField extends React.Component {
    constructor(props) {
        super(props)

    }

    render() {
        let email = this.props.value;
        let invalid = this.props.valid === false;

        return <div>
            <dt className="mma-section__list--title">
                <label className="label" htmlFor="renew-email">Email address</label>
            </dt>
            <dd className="mma-section__list--content">
                <input id="renew-email" value={email} onChange={this.props.handleChange} className="input-text" />
                {invalid && <p className="mma-error"> Please enter a valid email address</p>}
            </dd>
        </div>
    }
}

const Payment = ({ paymentType, handlePaymentType, showPaymentType, ...props }) => {

    return <div>{showPaymentType &&
        <PaymentType handlePaymentType={handlePaymentType} paymentType={paymentType} />}
        {this.props.paymentType === DIRECT_DEBIT &&
            <DirectDebit {...props} />}
    </div>
}

const PaymentType = ({ handlePayment, paymentType }) => <div>
    <dt className="mma-section__list--title">Payment method</dt>
    <dd className="mma-section__list--content option__label">
        <div style={{ boxSizing: 'unset' }}>
            <RadioGroup onChange={handlePaymentType} value={paymentType} horizontal>
                <RadioButton value={DIRECT_DEBIT}>Direct Debit</RadioButton>
                <RadioButton value={STRIPE}>Credit Card</RadioButton>
            </RadioGroup>
        </div>
    </dd>
</div>



const PlanChooser = ({ plans, selected, handleChange }) => {
    let renderedPlans = plans.map((plan) => <RadioButton key={plan.id} value={plan.id}><Plan price={plan.price} promotionalPrice={plan.promotionalPrice} /></RadioButton>);
    return <div>
        <dt className="mma-section__list--title">Payment options</dt>
        <dd className="mma-section__list--content">
            <RadioGroup value={selected.id} onChange={handleChange}>
                {renderedPlans}
            </RadioGroup>
        </dd>
    </div>
}

const Plan = ({ promotionalPrice, price }) => {
    if (promotionalPrice) {
        return <span className="option__label">
            <s>{props.price}</s>
            <strong>&nbsp;{promotionalPrice}</strong>
        </span>
    }
    return <span className="option__label">{price}</span>
}
