/**
 * Generic dropdown component
 * Can be used on any element
 * From membership-frontend
 *
 * Example usage:
 *     <button class="js-dropdown" data-dropdown-menu="js-foo"></button>
 *     <div id="js-foo" class="js-dropdown-menu is-hidden">all the foo (initially hidden)</div>
 *
 */
define(['$', 'bean'], function ($, bean) {
    'use strict';

    var DROPDOWN_CLASS = 'js-dropdown',
        DROPDOWN_SELECTOR = '.' + DROPDOWN_CLASS,
        HIDDEN_CLASS = 'is-hidden',
        MENU_SELECTOR = '.js-dropdown-menu',
        DROPDOWN_DISABLED_CLASS = 'js-dropdown-disabled';

    function bindHandlers() {
        bean.on(document, 'click', function (event) {
            var dropdown = closest(event.target, DROPDOWN_CLASS);
            var isHidden;

            if (!(dropdown && isEnabled(dropdown))) {
                return;
            }
            var $menu = $('#' + $(dropdown).data('dropdown-menu'));
            isHidden = $menu.hasClass(HIDDEN_CLASS);
            var $allDropdowns = $(DROPDOWN_SELECTOR);
            hideAllMenus();
            show($allDropdowns);
            if (isHidden) {
                show($menu);
                $allDropdowns.addClass(HIDDEN_CLASS);
                show($(dropdown));
            }

        });
    }

    function isEnabled(dropdown) {
        return !$(dropdown).hasClass(DROPDOWN_DISABLED_CLASS);
    }

    // bonzo doesn't provide this so had to write my own
    function closest(elem, className) {
        do {
            if ($(elem).hasClass(className)) {
                return elem;
            }
            elem = elem.parentElement;
        } while (elem);

        return null;
    }

    function hideAllMenus() {
        $(MENU_SELECTOR).addClass(HIDDEN_CLASS);
    }

    function show($elem) {
        $elem.removeClass(HIDDEN_CLASS);
    }

    function init() {
        if ($(DROPDOWN_SELECTOR).length > 0) {
            bindHandlers();
        }
    }

    return {
        init: init
    };

});
