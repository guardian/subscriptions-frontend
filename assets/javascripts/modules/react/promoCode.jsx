import React from 'react'

export const status = {
    VALID:"VALID",
    INVALID:"INVALID",
    NOTCHECKED:"NOTCHECKED",
    LOADING:"LOADING"
};

export class PromoCode extends React.Component {
render(){
    let href = '//p/'+this.props.value+'/terms/'
    return <div>
        <dt className="mma-section__list--title">
            <label className="label" for="promo">Promo Code</label>
        </dt>
        <dd className="mma-section__list--content">
        <PromoField value={this.props.value} handler={this.props.handler}/>
        <PromoButton status={this.props.status} onClick={this.props.send} />
            {this.props.status == status.VALID && <p><strong>✓&nbsp;&nbsp;Promotion applied</strong></p>}
            {this.props.copy && <p>{this.props.copy}</p>}
            {this.props.status == status.VALID && <a className="u-link" href={href}>Terms and conditions</a>}
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
                return 'Valid';
            }
            if (this.props.status === status.INVALID){
                return 'Invalid';
            }
            return 'Apply';
        };
        return <button
            className={`button ${state()} button--large button--arrow-right`}
            onClick={this.props.onClick}>{text()}</button>
    }
}
class PromoField extends React.Component {
    render(){
        return <input className="input-text input-text--promo" value={this.props.value} onChange={this.props.handler}/>
    }

}
