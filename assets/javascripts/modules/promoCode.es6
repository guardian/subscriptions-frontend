import ajax from 'ajax';
import jsRoutes from './jsRoutes'

export function validatePromoCode(promoCode, country, currency){
    return new Promise((resolve,reject)=>{
        let route = jsRoutes.controllers.Promotion.validate(promoCode, country, currency);
        ajax({
            type: 'json',
            method: route.method,
            url: route.url
        }).then((r)=>{
            resolve(r);
        },(f,a)=>{
            console.log('f',f,a);
            reject(f);
        })
    })
}

export function combinePromotionAndPlans(promotion, plans) {
    let newPlans = promotion.adjustedRatePlans;
    if (newPlans == null){
        return plans;
    }
    return plans.map((plan) => {
        if (plan.id in newPlans) {
            return Object.assign({},plan, {promotionalPrice: newPlans[plan.id]})
        }
        else {
            return plan;
        }
    })
}

