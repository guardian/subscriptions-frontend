define(['utils/user'], function (userUtil) {
    'use strict';

    var IDENTITY_MENU = '.js-identity-menu';
    var IDENTITY_TOGGLE = '.js-identity-menu-toggle';

    var ACTIVE_CLASS = 'is-active';
    var TOGGLE_CLASS = 'control--toggle';

    function addMenuListeners(menu, toggle) {
        toggle.classList.add(TOGGLE_CLASS);

        var listener = toggle.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopImmediatePropagation();

            toggle.classList.toggle(ACTIVE_CLASS);
            menu.classList.toggle(ACTIVE_CLASS);
        });

        if(menu.classList.contains(ACTIVE_CLASS)) {
            document.removeEventListener('click', listener);
        } else {
            document.documentElement.addEventListener('click', function() {
                menu.classList.remove(ACTIVE_CLASS);
                toggle.classList.remove(ACTIVE_CLASS);
            });
        }
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
