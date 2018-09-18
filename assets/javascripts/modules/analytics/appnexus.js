define([
    'modules/analytics/analyticsEnabled',
    'modules/analytics/dntEnabled'
], function (analyticsEnabled, dntEnabled) {
    'use strict';

    var segmentedPageCodes = {
        'digital': {
            'Landing': '8326260',
            'Checkout': '14355506',
            'Thankyou': '1025669'
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
        },
        'weekly': {
            'Landing': '8354456',
            'Checkout': '8354460',
            'Thankyou': '838548'
        }
    };

    function init() {
        if (!(guardian && guardian.pageInfo && guardian.pageInfo.productData && guardian.pageInfo.productData.productSegment)) { return; }

        var productSegment = guardian.pageInfo.productData.productSegment;

        var pageCodes = segmentedPageCodes[productSegment];
        if (!pageCodes) { return; }

        var pageType = guardian.pageInfo.pageType;
        if (!pageType) { return; }

        var pixelPath = (pageType === 'Thankyou') ? 'px' : 'seg';
        var parameterName = (pageType === 'Thankyou') ? 'id' : 'add';

        var parameterValue = pageCodes[pageType];

        var oImg = document.createElement('img');
        oImg.setAttribute('height', '0');
        oImg.setAttribute('width', '0');
        document.body.appendChild(oImg);

        // Marketing are reporting the thankyou pixel recording two conversions happening at the same time.
        // So in an attempt to mitigate, set the src attribute _after_ adding to the DOM to ensure no uncached preload.
        oImg.setAttribute('src', 'https://secure.adnxs.com/' + pixelPath + '?t=2&' + parameterName + '=' + parameterValue);
    }

    return {
        init: analyticsEnabled(dntEnabled(init))
    };
});
