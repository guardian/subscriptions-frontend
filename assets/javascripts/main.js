require([
    'modules/checkout',
    'modules/digitalpack',
    'utils/ajax'
], function(
    checkout,
    digitalpack,
    ajax
) {
    'use strict';

    // Global

    ajax.init({page: {ajaxUrl: ''}});
    checkout.init();
    digitalpack.init();
});
