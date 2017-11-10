const webpack = require("webpack");

const path = require('path');
 module.exports = /* function(debug) { return */{
    entry: {
        main: "./main"
    },
    output: {
        path: path.resolve('public/'),
        chunkFilename:  'webpack/[chunkhash].js',
        filename: "javascripts/[name].min.js",
        publicPath: '/assets/'
    },
    resolve: {
        modules: [
          path.resolve(__dirname, "node_modules"),
          path.resolve(__dirname, "assets", "javascripts"),
          path.resolve(__dirname, "assets", "..", "node_modules"),
          path.resolve(__dirname, "test")
        ],
        extensions: [".js", ".es6", '.jsx'],
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
                loader: 'babel-loader',
                query: {
                    "presets": [
                        ["env", {
                            "targets": {
                                "browsers": ["last 2 versions", "safari >= 7"]
                            }
                        }]
                    ],
                    cacheDirectory: '',
                    plugins: ["transform-object-rest-spread","transform-flow-strip-types", ["transform-runtime", {
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
                    plugins: ["transform-object-rest-spread","transform-flow-strip-types", ["transform-runtime", {
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

          watch: false,

    stats: {
        modules: true,
        reasons: true,
        colors: true
    },

    context: path.join(__dirname, 'assets', 'javascripts'),
    devtool: 'source-map'
}/*}*/;
