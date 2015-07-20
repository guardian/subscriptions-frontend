define(['modules/checkout/validations'], function (validations) {

    describe('#validatePaymentDetails', function () {

        it('should validate account number', function () {

            var tooShort = {
                accountNumber: '123',
                accountHolderName: '',
                sortCodeParts: [],
                detailsConfirmed: false
            };
            var tooLong = {
                accountNumber: '123456789101112',
                accountHolderName: '',
                sortCodeParts: [],
                detailsConfirmed: false
            };
            var justRightLow = {
                accountNumber: '123456',
                accountHolderName: '',
                sortCodeParts: [],
                detailsConfirmed: false
            };
            var justRightHigh = {
                accountNumber: '1234567890',
                accountHolderName: '',
                sortCodeParts: [],
                detailsConfirmed: false
            };

            expect((validations.validatePaymentDetails(tooShort)).accountNumberValid).toBe(false);
            expect((validations.validatePaymentDetails(tooLong)).accountNumberValid).toBe(false);
            expect((validations.validatePaymentDetails(justRightLow)).accountNumberValid).toBe(true);
            expect((validations.validatePaymentDetails(justRightHigh)).accountNumberValid).toBe(true);
        });

        it('should validate account holder name', function () {
            var tooLong = {
                accountNumber: '',
                accountHolderName: 'This name is longer than 18 characters',
                sortCodeParts: [],
                detailsConfirmed: false
            };
            var valid = {
                accountNumber: '',
                accountHolderName: 'Example Name',
                sortCodeParts: [],
                detailsConfirmed: false
            };
            expect((validations.validatePaymentDetails(tooLong)).accountHolderNameValid).toBe(false);
            expect((validations.validatePaymentDetails(valid)).accountHolderNameValid).toBe(true);
        });

        it('should validate sort code', function () {

            var tooShort = {
                accountNumber: '',
                accountHolderName: '',
                sortCodeParts: ['01', '01'],
                detailsConfirmed: false
            };
            var tooLong = {
                accountNumber: '',
                accountHolderName: '',
                sortCodeParts: ['01', '01', '1000'],
                detailsConfirmed: false
            };
            var justRight = {
                accountNumber: '',
                accountHolderName: '',
                sortCodeParts: ['01', '01', '01'],
                detailsConfirmed: false
            };

            expect((validations.validatePaymentDetails(tooShort)).sortCodeValid).toBe(false);
            expect((validations.validatePaymentDetails(tooLong)).sortCodeValid).toBe(false);
            expect((validations.validatePaymentDetails(justRight)).sortCodeValid).toBe(true);

        });

        it('should validate all details', function () {

            var valid = validations.validatePaymentDetails({
                accountNumber: '12346789',
                accountHolderName: 'Example Name',
                sortCodeParts: ['01', '01', '01'],
                detailsConfirmed: true
            });

            var invalid = validations.validatePaymentDetails({
                accountNumber: '12346789',
                accountHolderName: 'This name is longer than 18 characters',
                sortCodeParts: ['01', '01', '100', '10000'],
                detailsConfirmed: true
            });

            expect(valid.accountNumberValid).toBe(true);
            expect(valid.accountHolderNameValid).toBe(true);
            expect(valid.sortCodeValid).toBe(true);
            expect(valid.detailsConfirmedValid).toBe(true);
            expect(valid.allValid).toBe(true);

            expect(invalid.accountNumberValid).toBe(true);
            expect(invalid.accountHolderNameValid).toBe(false);
            expect(invalid.sortCodeValid).toBe(false);
            expect(invalid.detailsConfirmedValid).toBe(true);
            expect(invalid.allValid).toBe(false);

        });
    });
});
