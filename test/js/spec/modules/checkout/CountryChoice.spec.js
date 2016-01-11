define(['modules/checkout/countryChoice'], function (addressRules) {

    'use strict';

    describe('CountryChoice option-parser', function() {

        var option;
        beforeEach(function() {
            option = document.createElement('option');
        });

        it('should parse data-postcode-required and data-postcode-label from an element', function () {
            option.setAttribute('data-postcode-required', true);
            option.setAttribute('data-postcode-label', 'postcode');
            expect(addressRules.addressRules(option).postcode).toEqual({label: 'postcode', required: true});

            option.setAttribute('data-postcode-required', false);
            option.setAttribute('data-postcode-label', 'zip');
            expect(addressRules.addressRules(option).postcode).toEqual({label: 'zip', required: false});
        });

        it('should parse data-subdivision-required and data-subdivision-list from an element', function() {
            option.setAttribute('data-subdivision-required', true);
            option.setAttribute('data-subdivision-list', 'kent,essex');
            option.setAttribute('data-subdivision-label', 'County');
            expect(addressRules.addressRules(option).subdivision).toEqual({required: true, values: ['kent', 'essex'], label: 'County'});
        });

        it('should parse data-subdivision-required and data-subdivision-list from an element', function() {
            option.setAttribute('data-subdivision-required', false);
            option.setAttribute('data-subdivision-label', 'Province');
            expect(addressRules.addressRules(option).subdivision).toEqual({required: false, values: [], label: 'Province'});
        });
    });
});
