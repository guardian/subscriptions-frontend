console.log('start');
define(['src/modules/checkout/regex'], function (regex) {

    describe('Checkout form validation regex', function () {

        it('email validation', function () {
            expect(regex.isValidEmail("rt@tg.com")).toBe(true);
            expect(regex.isValidEmail("rt1234@tg.com")).toBe(true);
            expect(regex.isValidEmail("rt@tgcom")).toBe(false);
            expect(regex.isValidEmail("rttg.com")).toBe(false);
        });

        it('postcode validation', function () {
            expect(regex.isPostcode("M1 1AA")).toBe(true);
            expect(regex.isPostcode("M1$1AA")).toBe(false);
            expect(regex.isPostcode("M1")).toBe(false);
        });

        it('number validation', function () {
            expect(regex.isNumber("111111111")).toBe(true);
            expect(regex.isNumber("10")).toBe(true);
            expect(regex.isNumber("-10")).toBe(false);
            expect(regex.isNumber("asdaaa")).toBe(false);
            expect(regex.isNumber("£232")).toBe(false);
        });
    });
});
