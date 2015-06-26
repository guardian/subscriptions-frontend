console.log('start');
define(['src/modules/checkout/regex'], function (regex) {

    describe('Checkout form validation regex', function () {

        it('email validation', function () {
            expect(regex.isValidEmail("rt@tg.com")).toBe(true);
            expect(regex.isValidEmail("rt1234@tg.com")).toBe(true);
            expect(regex.isValidEmail("rt@tgcom")).toBe(false);
            expect(regex.isValidEmail("rttg.com")).toBe(false);
        });
    });
});
