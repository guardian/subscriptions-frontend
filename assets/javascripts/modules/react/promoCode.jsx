import React from 'react'

export const status = {
    VALID:"VALID",
    INVALID:"INVALID",
    NOTCHECKED:"NOTCHECKED",
    LOADING:"LOADING"
};

export class PromoCode extends React.Component {
render(){
    return <div>
        <PromoField value={this.props.value} handler={this.props.handler}/>
        <PromoButton status={this.props.status} onClick={this.props.send} />
<p>{this.props.status}</p>
    </div>
}

}
class PromoButton extends React.Component {
    render(){
        return <button onClick={this.props.onClick}>HEY</button>
    }
}
class PromoField extends React.Component {
    render(){
        return <input value={this.props.value} onChange={this.props.handler}/>
    }

}
