const merge = require('webpack-merge')
const common = require('./webpack.common.js')
const Uglify = require("webpack/lib/optimize/UglifyJsPlugin");
const CircularDependencyPlugin = require('circular-dependency-plugin');
module.exports = merge(common, {
    plugins: [
        new CircularDependencyPlugin({
            // exclude detection of files based on a RegExp
            exclude: /a\.js|node_modules/,
            // add errors to webpack instead of warnings
            failOnError: true
        })]
})
