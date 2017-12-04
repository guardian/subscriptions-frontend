import React from 'react'

export const status = {
    VALID:'VALID',
    INVALID:'INVALID',
    NOTCHECKED:'NOTCHECKED',
    LOADING:'LOADING'
};

export class PromoCode extends React.Component {
render(){
    return <div>
        <dt className="mma-section__list--title">
            <label className="label" htmlFor="promoCode">Promo code</label>
        </dt>
        <dd className="mma-section__list--content">
            <PromoField value={this.props.value} handler={this.props.handler} />
            <PromoButton status={this.props.status} onClick={this.props.send} />
            {this.props.status == status.VALID && <div className="u-note">{this.props.copy}</div>}
            {this.props.status == status.INVALID && <div className="u-error">{this.props.copy}</div>}
        </dd>
    </div>
}

}
class PromoButton extends React.Component {
    render(){
        let state = ()=>{
            if (this.props.status === status.VALID){
                return 'button--promo__valid';
            }
            if (this.props.status === status.INVALID){
                return 'button--promo__invalid';
            }
            return 'button--primary';
        };
        let text = ()=>{
            if (this.props.status === status.VALID){
                return 'Promotion Applied';
            }
            if (this.props.status === status.INVALID){
                return 'Invalid';
            }
            return 'Apply';
        };
        return <button
            className={`button ${state()} button--large button--arrow-right grid__item`}
            onClick={this.props.onClick}>{text()}</button>
    }
}
class PromoField extends React.Component {
    render(){
        return <input name="promoCode" className="input-text input-text--promo grid__item" value={this.props.value} onChange={this.props.handler}/>
    }

}
