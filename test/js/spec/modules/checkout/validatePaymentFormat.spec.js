define(['modules/checkout/validatePaymentFormat'], function (validatePaymentFormat) {

    describe('#validatePaymentFormat', function () {
        it('should validate account number', function () {

            var tooShort = {
                paymentMethod: 'direct-debit',
                accountNumber: '123',
                accountHolderName: '',
                sortCode: null,
                detailsConfirmed: false
            };
            var tooLong = {
                paymentMethod: 'direct-debit',
                accountNumber: '123456789101112',
                accountHolderName: '',
                sortCode: null,
                detailsConfirmed: false
            };
            var justRightLow = {
                paymentMethod: 'direct-debit',
                accountNumber: '123456',
                accountHolderName: '',
                sortCode: null,
                detailsConfirmed: false
            };
            var justRightHigh = {
                paymentMethod: 'direct-debit',
                accountNumber: '1234567890',
                accountHolderName: '',
                sortCode: null,
                detailsConfirmed: false
            };

            expect((validatePaymentFormat(tooShort)).accountNumberValid).toBe(false);
            expect((validatePaymentFormat(tooLong)).accountNumberValid).toBe(false);
            expect((validatePaymentFormat(justRightLow)).accountNumberValid).toBe(true);
            expect((validatePaymentFormat(justRightHigh)).accountNumberValid).toBe(true);
        });

        it('should validate account holder name', function () {
            var tooLong = {
                paymentMethod: 'direct-debit',
                accountNumber: '',
                accountHolderName: 'This name is longer than 18 characters',
                sortCode: null,
                detailsConfirmed: false
            };
            var valid = {
                paymentMethod: 'direct-debit',
                accountNumber: '',
                accountHolderName: 'Example Name',
                sortCode: null,
                detailsConfirmed: false
            };
            expect((validatePaymentFormat(tooLong)).accountHolderNameValid).toBe(false);
            expect((validatePaymentFormat(valid)).accountHolderNameValid).toBe(true);
        });

        it('should validate sort code', function () {

            var tooShort = {
                paymentMethod: 'direct-debit',
                accountNumber: '',
                accountHolderName: '',
                sortCode: '00-00',
                detailsConfirmed: false
            };
            var tooLong = {
                paymentMethod: 'direct-debit',
                accountNumber: '',
                accountHolderName: '',
                sortCode: '01-01-0100',
                detailsConfirmed: false
            };
            var justRight = {
                paymentMethod: 'direct-debit',
                accountNumber: '',
                accountHolderName: '',
                sortCode: '01-01-01',
                detailsConfirmed: false
            };

            expect((validatePaymentFormat(tooShort)).sortCodeValid).toBe(false);
            expect((validatePaymentFormat(tooLong)).sortCodeValid).toBe(false);
            expect((validatePaymentFormat(justRight)).sortCodeValid).toBe(true);

        });

        it('should validate all details', function () {

            var valid = validatePaymentFormat({
                paymentMethod: 'direct-debit',
                accountNumber: '12346789',
                accountHolderName: 'Example Name',
                sortCode: '01-01-01',
                detailsConfirmed: true
            });

            var invalid = validatePaymentFormat({
                paymentMethod: 'direct-debit',
                accountNumber: '12346789',
                accountHolderName: 'This name is longer than 18 characters',
                sortCode: '01-01-0100',
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
