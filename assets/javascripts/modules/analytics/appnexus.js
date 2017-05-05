define([
    'modules/analytics/analyticsEnabled',
    'modules/analytics/dntEnabled'
], function (analyticsEnabled, dntEnabled) {
    'use strict';

    var segmentedPageCodes = {
        'digital': {
            'Landing': '8326260',
            'Checkout': '8326264',
            'Thankyou': '836984'
        },
        'paper-digital': {
            'Landing': '8326267',
            'Checkout': '8326269',
            'Thankyou': '836986'
        },
        'paper': {
            'Landing': '8326272',
            'Checkout': '8326273',
            'Thankyou': '836985'
        }
    };

    function init() {
        if (!(guardian && guardian.pageInfo && guardian.pageInfo.productData && guardian.pageInfo.productData.productSegment)) { return; }

        var productSegment = guardian.pageInfo.productData.productSegment;
        var pixelPath = (productSegment === 'Thankyou') ? 'px' : 'seg';
        var parameterName = (productSegment === 'Thankyou') ? 'id' : 'add';

        var pageCodes = segmentedPageCodes[productSegment];
        if (!pageCodes) { return; }

        var pageType = guardian.pageInfo.pageType;
        if (!pageType) { return; }

        var parameterValue = pageCodes[pageType];

        var oImg = document.createElement('img');
        oImg.setAttribute('src', 'https://secure.adnxs.com/' + pixelPath + '?t=2&' + parameterName + '=' + parameterValue);
        oImg.setAttribute('alt', 'AppNexus pixel');
        oImg.setAttribute('height', '0');
        oImg.setAttribute('width', '0');
        document.body.appendChild(oImg);
    }

    return {
        init: analyticsEnabled(dntEnabled(init))
    };
});
