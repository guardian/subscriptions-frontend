var tests = [];

for (var file in window.__karma__.files) {
    if (window.__karma__.files.hasOwnProperty(file)) {
        if (/\.spec\.js$/.test(file)) {
            tests.push(file);
        }
    }
}


// docs: http://karma-runner.github.io/0.8/plus/RequireJS.html
requirejs.config({
    /**
     * Karma serves files from '/base'
     */
    baseUrl: '/base/assets/javascripts/',

    /**
     * Keep these in sync with the paths
     * found in the requirejs Grunt config
     * in Gruntfile.js
     */
    paths: {
        '$': 'utils/$',
        'Promise': 'bower_components/native-promise-only/lib/npo.src',
        'bean': 'bower_components/bean/bean',
        'bonzo': 'bower_components/bonzo/bonzo',
        'lodash': 'bower_components/lodash-amd/modern',
        'qwery': 'bower_components/qwery/qwery',
        'raven': 'bower_components/raven-js/dist/raven',
        'requireLib': 'bower_components/requirejs/require',
        'reqwest': 'bower_components/reqwest/reqwest'
    },

    /**
     * Ask require.js to load these files (all our tests)
     */
    deps: tests,

    /**
     * Start test run, once require.js is done
     */
    callback: window.__karma__.start
});
