define(['modules/checkout/addressFields'], function (addressFields) {

    'use strict';

    var dom = function(string) {
        var div = document.createElement('div');
        div.innerHTML = string;
        return div.firstChild;
    };

    describe('Address fields generator', function() {

        it('should generate a compulsory postcode field with the right label', function () {
            expect(addressFields.postcode(true, 'Postcode').isEqualNode(dom(
                '<div class="form-field">' +
                    '<label for="address-postcode" class="label">Postcode</label>' +
                    '<input type="text" id="address-postcode" name="personal.address.postcode" required="required">' +
                '</div>'
                ))
            ).toBeTruthy();
        });

        it('should generate an optional postcode field with the right label', function () {
            expect(addressFields.postcode(false, 'Zipcode').isEqualNode(dom(
                '<div class="form-field">' +
                    '<label for="address-postcode" class="optional-marker label">Zipcode</label>' +
                    '<input type="text" id="address-postcode" name="personal.address.postcode">' +
                '</div>'
            ))).toBeTruthy();
        });

        it('should generate a freeform region box when no list is supplied', function () {
            expect(addressFields.subdivision(true, 'Province', []).isEqualNode(dom(
                '<div class="form-field">' +
                    '<label for="address-subdivision" class="label">Province</label>' +
                    '<input type="text" id="address-subdivision" name="personal.address.subdivision" required="required">' +
                '</div>'
            ))).toBeTruthy();
        });

        it('should generate a select box when a list is supplied', function () {
            expect(addressFields.subdivision(true, 'Province', ['Bromley']).isEqualNode(dom(
                '<div class="form-field">' +
                    '<label for="address-subdivision" class="label">Province</label>' +
                    '<select id="address-subdivision" name="personal.address.subdivision" required="required">' +
                        '<option></option>' +
                        '<option value="Bromley">Bromley</option>' +
                    '</select>' +
                '</div>'
            ))).toBeTruthy();
        });
    });
});
