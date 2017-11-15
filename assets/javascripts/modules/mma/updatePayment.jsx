import React from 'react'
import ReactDOM from 'react-dom';
const timeout = (t) => new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
        clearTimeout(timer)
        reject("Timeout")
    }, t)
})
export const init = (elem) => {
    let handler = window.StripeCheckout.configure(guardian.stripeCheckout)
    ReactDOM.render(<Payment sub={elem.dataset.subId} email={elem.dataset.email} phone={elem.dataset.phone} stripe={handler} product={elem.dataset.product} />, elem)
}

const handleStripeResponse = (t, url, key) => {
    let form = new FormData();
    form.append('stripeToken', t.id)
    form.append('publicKey', key)
    return fetch(url,
        {
            method: 'post',
            credentials: 'include',
            mode: 'cors',
            headers: {
                'Csrf-Token': 'nocheck',
            },
            body: form
        }).then(resp => resp.json())
}



const getDetails = async (url) => {
    let resp = await fetch(url,
        {
            method: 'get', credentials: 'include'
        })
    let json = await resp.json()
    return json
}

const OPEN = 'open'
const WAITING = 'waiting'
const SUCCESS = 'success'
const FAILURE = 'failure'
const FORM = 'form'

const CardUpdate = ({ card, handler }) => (<div><button className="button button--primary button--large" onClick={handler}>•••• •••• •••• {card.last4} Update Card</button></div>)
const Success = () => (<p>Thank you, we have successfully updated your payment details.</p>)
const Failure = ({ phone }) => <p>Unfortunately, we are unable to update your payment details at this time, please contact the call centre. {phone}</p>


const Waiting = () => (<div className="loader js-loader is-loading">Processing&hellip;</div>)

class Payment extends React.Component {

    constructor(props) {
        super(props)
        const endpoint = props.product === 'digitalpack' ? 'digitalpack' : 'paper'
        console.log(props, endpoint)
        const url = guardian.members_data_api
        const dataUrl = `${url}/user-attributes/me/mma-${endpoint}`
        const cardUrl = `${url}/user-attributes/me/${endpoint}-update-card`

        const token = (t) => {
            this.setState({ state: WAITING })
            handleStripeResponse(t, cardUrl, this.state.stripePublicKeyForUpdate).then(json => {
                this.setState({ state: SUCCESS })
            })
                .catch(() => {
                    this.setState({ state: FAILURE })
                })
        }

        this.handler = () => {
            this.setState({ state: OPEN })
            this.props.stripe.open({
                key: this.state.card.stripePublicKeyForUpdate,
                email: this.props.email,
                token: token,
                panelLabel: "Update",
                closed: () => { if (this.state.state == OPEN) this.setState({ state: FORM }) }
            })
        }
        this.state = {
            state: WAITING,
            card: null
        }

        Promise.race([getDetails(dataUrl), timeout(10000)]).then(resp => {
            let sub = resp.subscription && resp.subscription.subscriberId
            if (!sub || sub != this.props.sub || !this.props.stripe) {
                this.setState({ state: FAILURE })
                return
            }
            this.setState({ state: FORM, card: resp.subscription.card })

        })
            .catch((e) => {
                this.setState({ state: FAILURE })
            })
    }

    render() {
        return <div>
            {this.state.state == FORM && <CardUpdate card={this.state.card} handler={this.handler} />}
            {this.state.state == WAITING && <Waiting />}
            {this.state.state == OPEN && <Waiting />}
            {this.state.state == SUCCESS && <Success />}
            {this.state.state == FAILURE && <Failure phone={this.props.phone} />}
        </div>
    }
}

