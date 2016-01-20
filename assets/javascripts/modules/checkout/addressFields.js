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
        input.classList.add('input-text');
        input.type = 'text';
        return input;
    };

    var selectInput = function (required, name, values) {
        var select = formField(required, name, 'select');
        select.classList.add('select--wide');
        select.appendChild(document.createElement('option'));
        values.forEach(function(value) {
            var o = document.createElement('option');
            o.innerHTML = value;
            o.value = value;
            select.appendChild(o);
        });
        return select;
    };


    var postcode = function (required, text) {
        return {
            label: label(required, text, 'postcode'),
            input: textInput(required, 'postcode')
        };
    };

    var subdivision = function (required, text, values) {
        var name = 'subdivision';
        return {
            label: label(required, text, name),
            input: values.length ? selectInput(required, name, values) : textInput(required, name)
        };
    };

    return {
        postcode: postcode,
        subdivision: subdivision
    };
});
