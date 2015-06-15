module.exports = function(grunt) {
    'use strict';

    var isDev = (grunt.option('dev') !== undefined) ? Boolean(grunt.option('dev')) : process.env.GRUNT_ISDEV === '1';
    var pkg = grunt.file.readJSON('package.json');

    if (isDev) {
        grunt.log.subhead('Running Grunt in DEV mode');
    }

    grunt.initConfig({
        pkg: pkg,

        dirs: {
            assets: {
                root:        'app/assets',
                javascripts: '<%= dirs.assets.root %>/javascripts',
                stylesheets: '<%= dirs.assets.root %>/stylesheets'
            },
            public: {
                root:        'public',
                javascripts: '<%= dirs.public.root %>/javascripts',
                stylesheets: '<%= dirs.public.root %>/stylesheets'
            }
        },

        sass: {
            compile: {
                files: {
                    '<%= dirs.public.stylesheets %>/main.min.css': '<%= dirs.assets.stylesheets %>/main.scss',
                    '<%= dirs.public.stylesheets %>/ie9.min.css': '<%= dirs.assets.stylesheets %>/ie9.scss',
                    '<%= dirs.public.stylesheets %>/ie-old.min.css': '<%= dirs.assets.stylesheets %>/ie-old.scss'
                },
                options: {
                    style: 'compressed'
                }
            }
        },

        requirejs: {
            compile: {
                options: {
                    name: 'main',
                    include: [
                        'requireLib'
                    ],
                    baseUrl: '<%= dirs.assets.javascripts %>',
                    paths: {
                        '$': 'utils/$',
                        'bean': 'bower_components/bean/bean',
                        'bonzo': 'bower_components/bonzo/bonzo',
                        'qwery': 'bower_components/qwery/qwery',
                        'requireLib': 'bower_components/requirejs/require',
                    },
                    optimize: isDev ? 'none' : 'uglify2',
                    generateSourceMaps: isDev ? 'true' : 'false',
                    preserveLicenseComments: false,
                    out: '<%= dirs.public.javascripts %>/main.min.js'
                }
            }
        },

        watch: {
            css: {
                files: ['<%= dirs.assets.stylesheets %>/**/*.scss'],
                tasks: ['sass']
            },
            js: {
                files: ['<%= dirs.assets.javascripts %>/**/*.js'],
                tasks: ['requirejs']
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-sass');
    grunt.loadNpmTasks('grunt-contrib-requirejs');
    grunt.loadNpmTasks('grunt-contrib-watch');

    grunt.registerTask('compile', ['sass', 'requirejs']);
    grunt.registerTask('default', ['compile']);
};
