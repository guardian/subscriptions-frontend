import React from 'react'
import ReactDOM from 'react-dom';



export const init = (elem) => {
    let handler = window.StripeCheckout.configure(guardian.stripeCheckout)
    ReactDOM.render(<Payment sub={elem.dataset.subId} email={elem.dataset.email} stripe={handler} />, elem)
}

const getDetails = async (url) => {
    let resp = await fetch(`${url}/user-attributes/me/mma-paper`, { method: 'get', credentials: 'include' })
    let json = await resp.json()
    return json
}
const WAITING = 'waiting'
const SUCCESS = 'success'
const FAILURE = 'failure'
const FORM = 'form'

const CardUpdate = ({ card, handler }) => (<div><button className="button button--primary button--large" onClick={handler}>•••• •••• •••• {card.last4} Update Card</button></div>)
const Success = () => (<p>Thank you, we have successfully updated your payment details.</p>)
const Failure = () => (<p>Unfortunately, we are unable to update your payment details at this time, please contact the call centre.</p>)
const Waiting = () => (<div className="loader js-loader is-loading">Processing&hellip;</div>)

class Payment extends React.Component {

    constructor(props) {
        super(props)
        const url = guardian.members_data_api

        const token = (t) => {
            let form = new FormData();
            form.append('stripeToken', t.id)
            form.append('publicKey', this.state.card.stripePublicKeyForUpdate)
            fetch(`${url}/user-attributes/me/paper-update-card`, {
                method: 'post',
                credentials: 'include',
                mode: 'cors',
                headers: {
                    'Csrf-Token': 'nocheck',
                },
                body: form
            }).then(resp => resp.json())
                .then(json => {
                    this.setState({ state: SUCCESS })
                })
                .catch(() => {
                    this.setState({ state: FAILURE })
                })
        }
        this.handler = () => {
            this.props.stripe.open({
                key: this.state.card.stripePublicKeyForUpdate,
                email: this.props.email,
                token: token
            })
            this.setState({ state: WAITING })
        }
        this.state = {
            state: WAITING,
            card: null
        }
        getDetails(url).then(resp => {
            let sub = resp.subscription && resp.subscription.subscriberId
            if (!sub || sub != this.props.sub || !this.props.stripe) {
                this.setState({ state: FAILURE })
                return
            }
            this.setState({ state: FORM, card: resp.subscription.card })

        }).catch((e) => {
            this.setState({ state: FAILURE })
        })
    }
    render() {
        return <div>
            {this.state.state == FORM && <CardUpdate card={this.state.card} handler={this.handler} />}
            {this.state.state == WAITING && <Waiting />}
            {this.state.state == SUCCESS && <Success />}
            {this.state.state == FAILURE && <Failure />}
        </div>
    }
}

