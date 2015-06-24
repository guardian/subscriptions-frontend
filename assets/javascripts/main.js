require([
    'modules/checkout/checkout',
    'modules/digitalpack'
], function(
    checkout,
    digitalpack
) {
    'use strict';

    // Global
    checkout.init();
    digitalpack.init();

});
