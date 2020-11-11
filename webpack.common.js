const webpack = require('webpack');

const path = require('path');
 module.exports = /* function(debug) { return */{
    entry: {
        main: './main'
    },
    output: {
        path: path.resolve('public/'),
        chunkFilename:  'webpack/[chunkhash].js',
        filename: 'javascripts/[name].min.js',
        publicPath: '/assets/'
    },
    resolve: {
        modules: [
          path.resolve(__dirname, 'node_modules'),
          path.resolve(__dirname, 'assets', 'javascripts'),
          path.resolve(__dirname, 'assets', '..', 'node_modules'),
          path.resolve(__dirname, 'test')
        ],
        extensions: ['.js', '.es6', '.jsx'],
        alias: {
            '$$': 'utils/$',
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
                exclude: [
                    {
                        test: /node_modules/,
                        exclude: [
                        /@guardian\/(?!(automat-modules))/,
                        ],
                    },
                ],
                loader: 'babel-loader'
            },
            {
                test: /\.jsx?$/,
                exclude: [
                    {
                        test: /node_modules/,
                        exclude: [
                        /@guardian\/(?!(automat-modules))/,
                        ],
                    },
                ],
                loader: 'babel-loader'
            },
            {
                test: /\.css$/,
                loader: 'style-loader!css-loader'
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
