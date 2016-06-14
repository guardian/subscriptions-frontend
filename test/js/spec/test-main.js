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
     * Ask require.js to load these files (all our tests)
     */
    deps: tests,

    /**
     * Start test run, once require.js is done
     */
    callback: window.__karma__.start
});
