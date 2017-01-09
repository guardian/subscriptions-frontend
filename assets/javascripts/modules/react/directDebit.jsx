import React from 'react'


export class DirectDebit extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        return <div>
            <SortCodeComponent value={this.props.sortCode} onChange={this.props.handleSortCode}
                               valid={this.props.validSortCode}/>
            <AccountNumber onChange={this.props.handleAccountNumber} value={this.props.AccountNumber}
                           valid={this.props.validAccountNumber}/>
            <AccountHolder onChange={this.props.handleAccountHolder} value={this.props.AccountHolder}
                           valid={this.props.validAccountHolder}/>
            <Confirmation checked={this.props.directDebitConfirmed} onChange={this.props.handleDirectDebitConfirmation}
                          valid={this.props.validDirectDebitConfirmed}/>
            <dt className="mma-section__list--title">
                Advance Notice
            </dt>
            <dd className="mma-section__list--content">
                <div className="mma-section__list--restricted prose">
                    <p>The details of your Direct Debit instruction including payment schedule, due date, frequency and
                        amount will be sent to you within three working days. All the normal Direct Debit safeguards and
                        guarantees apply.</p>
                    <p>The Guardian, Unit 16, Coalfield Way, Ashby Park, Ashby-De-La-Zouch, LE65 1JT United Kingdom
                    </p>
                    <p>Tel: +44 (0) 330 333 6767</p>
                    <p><a href="mailto:gwsubs@theguardian.com">gwsubs@theguardian.com</a></p>
                </div>
            </dd>
        </div>
    }

}

class SortCodeComponent extends React.Component {
    constructor(props) {
        super(props)

    }

    render() {
        let invalid = this.props.valid === false;
        return <div>
            <dt className="mma-section__list--title">
                <label className="label" for="payment-sortcode">Sort code</label>
            </dt>
            <dd className="mma-section__list--content">
                <input value={this.props.value} onChange={this.props.onChange}
                       type="text"
                       className="input-text js-input"
                       placeholder="00-00-00"
                />
                {invalid && <p className="mma-error">
                    Please enter a valid sort code
                </p>}
            </dd>
        </div>
    }
}

class AccountNumber extends React.Component {
    constructor(props) {
        super(props)

    }


    render() {
        let invalid = this.props.valid === false;
        return <div>
            <dt className="mma-section__list--title">
                <label className="label" for="payment-account">Account number</label>
            </dt>
            <dd className="mma-section__list--content">
                <input value={this.props.value}
                       onChange={this.props.onChange}
                       id="payment-account"
                       name="payment.account"
                       pattern="[0-9]*" minlength="6"
                       maxlength="10"
                       className="input-text"
                />
                {invalid && <p className="mma-error">
                    Please enter a valid bank account number
                </p>}
            </dd>
        </div>
    }
}

class AccountHolder extends React.Component {
    constructor(props) {
        super(props)

    }

    /*
     * BACS requirement:
     "The payer’s account name (maximum of 18 characters).
     This must be the name of the person who is paying the Direct Debit
     and has signed the Direct Debit Instruction (DDI)."
     http://www.bacs.co.uk/Bacs/Businesses/Resources/Pages/Glossary.aspx
     * */

    render() {
        let invalid = this.props.valid === false;

        return <div>
            <dt className="mma-section__list--title">
                <label for="payment-holder">Account holder</label>
            </dt>
            <dd className="mma-section__list--content">
                <input
                    id="payment-holder"
                    value={this.props.value}
                    onChange={this.props.onChange}
                    className="input-text"
                    maxLength="18"
                />
                {invalid && <p className="mma-error">
                    This field is required
                </p>}
            </dd>
        </div>
    }
}

class Confirmation extends React.Component {
    render() {
        let invalid = this.props.valid === false;
        return <div>
            <dt className="mma-section__list--title">
                Confirmation
            </dt>
            <dd className="mma-section__list--content">
                <div className="mma-section__list--restricted">

                    <label className="option">
                <span className="option__input">
                    <input type="checkbox"
                           onChange={this.props.onChange}
                           checked={this.props.checked}
                    />
                </span>
                        <span className="option__label">
                    I confirm that I am the account holder and I am solely able to authorise debit from the account
        </span>

                    </label>
                    {invalid && <p className="mma-error">
                        Please confirm that you are the account holder
                    </p>}
                </div>
            </dd>
        </div>
    }
}
