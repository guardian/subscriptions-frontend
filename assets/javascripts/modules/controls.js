define(['bean', 'utils/user'], function (bean, userUtil) {
    'use strict';

    var IS_ACTIVE = 'is-active';
    var IDENTITY_MENU = '.js-identity-menu';
    var IDENTITY_TOGGLE = '.js-identity-menu-toggle';

    function addMenuListeners(menu, toggle) {
        toggle.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopImmediatePropagation();

            toggle.classList.toggle(IS_ACTIVE);
            menu.classList.toggle(IS_ACTIVE);

            if(!menu.classList.contains(IS_ACTIVE)) {
                bean.off(menu, 'click');
            } else {
                bean.on(document.documentElement, 'click', function () {
                    menu.classList.remove(IS_ACTIVE);
                });
            }
        });
    }

    function populateReturnUrl(href, currentUrl) {
        return href.replace(/(returnUrl=[^&]+)/g, '$1' + currentUrl);
    }

    function setIdentityCtaReturnUrl(toggle) {
        var loc = window.location;
        var currentUrl = loc.pathname + loc.search;
        var newHref = populateReturnUrl(toggle.getAttribute('href'), currentUrl);
        toggle.setAttribute('href', newHref);
    }

    function init() {
        var menu = document.querySelector(IDENTITY_MENU);
        var toggle = document.querySelector(IDENTITY_TOGGLE);

        if(!(menu && toggle)) {
            return;
        }

        if (userUtil.isLoggedIn()) {
            addMenuListeners(menu, toggle);
        } else {
            setIdentityCtaReturnUrl(toggle);
        }
    }

    return {
        init: init
    };
});
