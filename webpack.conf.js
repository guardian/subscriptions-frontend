var Uglify = require("webpack/lib/optimize/UglifyJsPlugin");

module.exports = function(debug) { return {
    resolve: {
        root: ["assets/javascripts", "assets/../node_modules/", "test/"],
        extensions: ["", ".js", ".es6", '.jsx'],
        alias: {
            '$$': 'utils/$',
            'lodash': 'lodash-amd/modern',
            'bean': 'bean/bean',
            'bonzo': 'bonzo/bonzo',
            'qwery': 'qwery/qwery',
            'reqwest': 'reqwest/reqwest',
            'respimage': 'respimage/respimage',
            'lazySizes': 'lazysizes/lazysizes',
            'gumshoe': 'gumshoe/dist/js/gumshoe',
            'smoothScroll': 'smooth-scroll/dist/js/smooth-scroll',
            'ajax': 'utils/ajax'
        }
    },

    module: {
        loaders: [
            {
                test: /\.es6$/,
                exclude: /node_modules/,
                loader: 'babel',
                query: {
                    presets: ['es2015'],
                    cacheDirectory: ''
                }
            },
            {
                test: /\.jsx?$/,
                exclude: /node_modules/,
                loader: 'babel-loader',
                query: {
                    presets: ['es2015', 'react']
                }
            },
            {
                test: /\.css$/,
                loader: "style-loader!css-loader"
            }
        ]
    },

    plugins: !debug ? [
        new Uglify({compress: {warnings: false}})
    ] : [],

    progress: true,
    failOnError: true,
    watch: false,
    keepalive: false,
    inline: true,
    hot: false,

    stats: {
        modules: true,
        reasons: true,
        colors: true
    },

    context: 'assets/javascripts',
    debug: true,
    devtool: 'source-map'
}};
