var Uglify = require("webpack/lib/optimize/UglifyJsPlugin");

var webpack = require("webpack");

var path = require('path');

module.exports = function(debug) { return {
    resolve: {
        root: [
          path.join(__dirname, "node_modules"),
          path.join(__dirname, "assets", "javascripts"),
          path.join(__dirname, "assets", "..", "node_modules"),
          path.join(__dirname, "test")
        ],
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
                    "presets": [
                        ["env", {
                            "targets": {
                                "browsers": ["last 2 versions", "safari >= 7"]
                            }
                        }]
                    ],
                    cacheDirectory: '',
                    plugins: ["transform-flow-strip-types", ["transform-runtime", {
                        "helpers": false,
                        "polyfill": false,
                        "regenerator": true,
                        "moduleName": "babel-runtime"
                    }]]

}
            },
            {
                test: /\.jsx?$/,
                exclude: /node_modules/,
                loader: 'babel-loader',
                query: {
                    presets:  [["env", {
                        "targets": {
                            "browsers": ["last 2 versions", "safari >= 7"]
                        }
                    }], 'react'],
                    cacheDirectory: '',
                    plugins: ["transform-flow-strip-types", ["transform-runtime", {
                        "helpers": false,
                        "polyfill": false,
                        "regenerator": true,
                        "moduleName": "babel-runtime"
                    }]]
                }
            },
            {
                test: /\.css$/,
                loader: "style-loader!css-loader"
            }
        ]
    },

    plugins: !debug ? [
        new webpack.DefinePlugin({
            'process.env':{
                'NODE_ENV': JSON.stringify('production')
            }
        }),
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

    context: path.join(__dirname, 'assets', 'javascripts'),
    debug: true,
    devtool: 'source-map'
}};
