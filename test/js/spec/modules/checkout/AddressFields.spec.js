define(['modules/checkout/addressFields'], function (addressFields) {

    'use strict';

    var dom = function(string) {
        var div = document.createElement('div');
        div.innerHTML = string;
        return div.firstChild;
    };

    describe('Address fields generator', function() {

        it('should generate a compulsory postcode field with the right label', function () {
            var output = addressFields.postcode('personal.address.postcode', true, 'Postcode');

            expect(output.input.isEqualNode(dom(
                '<input type="text" id="personal-address-postcode" name="personal.address.postcode" class="input-text" required="required">'
            ))).toBeTruthy();

            expect(output.label.isEqualNode(dom(
                '<label for="personal-address-postcode" class="label">Postcode</label>'
            ))).toBeTruthy();
        });

        it('should generate an optional postcode field with the right label', function () {
            var output = addressFields.postcode('personal.address.postcode', false, 'Zipcode');
            expect(output.input.isEqualNode(dom(
                '<input type="text" id="personal-address-postcode" class="input-text" name="personal.address.postcode">'
            ))).toBeTruthy();

            expect(output.label.isEqualNode(dom(
                '<label for="personal-address-postcode" class="optional-marker label">Zipcode</label>'
            ))).toBeTruthy();
        });

        it('should generate a freeform region box when no list is supplied', function () {
            var output = addressFields.subdivision('personal.address.subdivision', true, 'Province', []);

            expect(output.input.isEqualNode(dom(
                '<input type="text" id="personal-address-subdivision" name="personal.address.subdivision" class="input-text" required="required">'
            ))).toBeTruthy();

            expect(output.label.isEqualNode(dom(
                '<label for="personal-address-subdivision" class="label">Province</label>'
            ))).toBeTruthy();
        });

        it('should generate a select box when a list is supplied', function () {
            var output = addressFields.subdivision('personal.address.subdivision', true, 'Province', ['Bromley']);

            expect(output.input.isEqualNode(dom(
                '<select id="personal-address-subdivision" name="personal.address.subdivision" class="select--wide" required="required">' +
                    '<option></option>' +
                    '<option value="Bromley">Bromley</option>' +
                '</select>'
            ))).toBeTruthy();
        });
    });
});
