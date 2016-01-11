define([], function () {
    'use strict';

    var label = function (required, text, forAttr) {
        var labelElement = document.createElement('label');
        labelElement.setAttribute('for', 'address-' + forAttr);

        if(!required) {
            labelElement.classList.add('optional-marker');
        }

        labelElement.classList.add('label');
        labelElement.textContent = text;
        return labelElement;
    };

    var formField = function(required, name, element) {
        var e = document.createElement(element);
        e.id = 'address-' + name;
        e.name = 'personal.address.' + name;

        if (required) {
            e.setAttribute('required', 'required');
        }

        return e;
    };

    var textInput = function (required, name) {
        var input = formField(required, name, 'input');
        input.type = 'text';
        return input;
    };

    var selectInput = function (required, name, values) {
        var input = formField(required, name, 'select');
        input.appendChild(document.createElement('option'));
        values.forEach(function(value) {
            var o = document.createElement('option');
            o.innerHTML = value;
            o.value = value;
            input.appendChild(o);
        });
        return input;
    };

    var container = function() {
        var elem = document.createElement('div');
        elem.classList.add('form-field');
        return elem;
    };


    var postcode = function (required, text) {
        var c = container();
        c.appendChild(label(required, text, 'postcode'));
        c.appendChild(textInput(required, 'postcode'));
        return c;
    };

    var subdivision = function (required, text, values) {
        var c = container();
        var name = 'subdivision';
        c.appendChild(label(required, text, name));
        var elem = values.length ? selectInput(required, name, values) : textInput(required, name);
        c.appendChild(elem);
        return c;
    };

    return {
        postcode: postcode,
        subdivision: subdivision
    };
});
