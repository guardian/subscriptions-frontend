import ajax from 'ajax';
import jsRoutes from './jsRoutes'

export function check(promoCode, country){
    return new Promise((resolve,reject)=>{
        let route = jsRoutes.controllers.Promotion.validate(promoCode,country);
        ajax({
            type: 'json',
            method: route.method,
            url: route.url
        }).then((r)=>{
            console.log('r',r);
            resolve(r);
        },(f,a)=>{
            console.log('f',f,a);
            reject(f);
        })
    })
}

