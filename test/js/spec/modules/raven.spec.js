define(['modules/raven', 'lodash/lang/isEqual'], function (raven, isEqual) {
    describe('Raven', function () {
        it('should return a tags object', function () {
            var tags = raven.getTags('DEV');
            expect(tags.build_number).toBe('DEV');
            expect(tags.userIdentityId).toBe(undefined);
        });
        it('should return an identity id in tags when signed in', function () {
            var tags = raven.getTags('1234', {
                id: '4321'
            });
            expect(tags.build_number).toBe('1234');
            expect(tags.userIdentityId).toBe('4321');
            expect(isEqual(tags, {
                build_number: '1234',
                userIdentityId: '4321'
            })).toBe(true);
        });
    });
});
