/*global Raven, s_gi */
define([
    '$',
    'bean'
], function ($, bean) {
    'use strict';

    var omniture, s;


    function bindLinkTracking() {
        $('a[data-link-tracking]').each(function (domElem) {
            bean.on(domElem, 'click', function () {
                trackLink($(this).attr('href'));
            });
        });
    }

    function trackLink(link) {
        omniture = omniture || init();
        omniture.then(function(){
            s.tl(true, 'o', link);
        });
    }

    function trackEvent(prop17, pageName, products) {
        omniture = omniture || init();

        omniture.then(function(){
            if (prop17) {
                s.prop17 = prop17;
                s.pageName = pageName;
                s.products = products;
                s.t();
            }
        });
    }

    function onSuccess() {
        var s_code;

        window.s_account = 'guardiangu-subscribe,guardiangu-network';
        s = s_gi('guardiangu-network');

        s.pageName = document.title;
        s.channel = 'Subscriber';
        s.prop3 = 'GU.co.uk';
        s.prop5 = 'Subscriber';
        s.prop11 = 'Subscriber';
        s.hier2 = 'GU/News/Subscriber/';
        s.prop14 = '24.0';
        s.prop19 = 'Subscriber';
        s.eVar19 = 'D=c19';
        s.prop42 = 'Subscriber';
        s.prop47 = 'UK';

        var main = document.querySelector('main');
        if(main) {
            var attributes = [].slice.call(main.attributes);
            attributes.forEach(function(attr){
                if(attr.name.indexOf('data-tracking-')>-1){
                    var prop = attr.name.replace('data-tracking-','');
                    s[prop] = attr.nodeValue;
                }
            });
        }

        s_code = s.t();

        if (s_code) {
            /*jslint evil: true */
            document.write(s_code);
        }

        bindLinkTracking();
    }

    function init() {
        omniture = omniture || require(['js!omniture'])
            .then(onSuccess, function (err) {
                Raven.captureException(err);
            });
        return omniture;
    }

    return {
        init: init,
        trackEvent: trackEvent
    };
});
