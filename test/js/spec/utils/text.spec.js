define(['utils/text'], function (textUtils) {

    describe('textUtils', function() {

        describe('#trimWhitespace', function() {
            it('should trim whitespace from the start and end of a string', function() {
                expect(textUtils.trimWhitespace("abcd")).toEqual("abcd");
                expect(textUtils.trimWhitespace("a b c d")).toEqual("a b c d");
                expect(textUtils.trimWhitespace(" a b c d  ")).toEqual("a b c d");
                expect(textUtils.trimWhitespace(" a bc d  e")).toEqual("a bc d  e");
                expect(textUtils.trimWhitespace(" abc")).toEqual("abc");
            });
        });

        describe('#removeWhitespace', function() {
            it('remove all whitespace from a string', function() {
                expect(textUtils.removeWhitespace("a")).toEqual("a");
                expect(textUtils.removeWhitespace("abcd")).toEqual("abcd");
                expect(textUtils.removeWhitespace("                 a")).toEqual("a");
                expect(textUtils.removeWhitespace("b                 ")).toEqual("b");
                expect(textUtils.removeWhitespace("a b c d")).toEqual("abcd");
                expect(textUtils.removeWhitespace(" a b c d  ")).toEqual("abcd");
                expect(textUtils.removeWhitespace(" a bc d  e")).toEqual("abcde");
                expect(textUtils.removeWhitespace(" abc")).toEqual("abc");
            });
        });

        describe('#mergeValues', function () {

            var mergeValues = textUtils.mergeValues;

            it('join values with provided separator', function () {
                expect(mergeValues(['one', 'two', 'three'], '')).toBe('onetwothree');
                expect(mergeValues(['one', 'two', 'three'], ' ')).toBe('one two three');
                expect(mergeValues(['one', 'two', 'three'], ', ')).toBe('one, two, three');
                expect(mergeValues(['one', 'two', 'three'], '-')).toBe('one-two-three');
            });
            it('exclude empty values', function () {
                expect(mergeValues(['one', '', 'three'], '')).not.toBe('onetwothree');
                expect(mergeValues(['one', '', 'three'], ' ')).toBe('one three');
                expect(mergeValues(['one', '', 'three'], ', ')).not.toBe('one, two, three');
                expect(mergeValues(['one', '', 'three'], ', ')).toBe('one, three');
                expect(mergeValues(['one', '', 'three'], '-')).not.toBe('one--three');
                expect(mergeValues(['one', '', 'three'], '-')).toBe('one-three');
            });

        });

    });



});

