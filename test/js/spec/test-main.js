var tests = ['modules/checkout/regex.spec'];

// docs: http://karma-runner.github.io/0.8/plus/RequireJS.html
requirejs.config({
    // Karma serves files from '/base'
    baseUrl: '/base/test/js/spec',

    // Keep these in sync with the paths found in the requireJs paths
    paths: {
        'src': '../../../assets/javascripts'
    },

    // ask Require.js to load these files (all our tests)
    deps: tests,

    // start test run, once Require.js is done
    callback: window.__karma__.start
});
