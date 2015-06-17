module.exports = function(grunt) {
    'use strict';

    require('time-grunt')(grunt);

    /**
     * Setup
     */
    var pkg = grunt.file.readJSON('package.json');
    var isDev = (grunt.option('dev') !== undefined) ? Boolean(grunt.option('dev')) : process.env.GRUNT_ISDEV === '1';

    /**
     * Load all grunt-* tasks
     */
    require('load-grunt-tasks')(grunt);

    if (isDev) {
        grunt.log.subhead('Running Grunt in DEV mode');
    }

    grunt.initConfig({
        pkg: pkg,

        dirs: {
            assets: {
                root:        'assets',
                javascripts: '<%= dirs.assets.root %>/javascripts',
                stylesheets: '<%= dirs.assets.root %>/stylesheets',
                images:      '<%= dirs.assets.root %>/images'
            },
            public: {
                root:        'public',
                javascripts: '<%= dirs.public.root %>/javascripts',
                stylesheets: '<%= dirs.public.root %>/stylesheets',
                images:      '<%= dirs.public.root %>/images'
            }
        },

        clean: {
            assets: ['<%= dirs.public.javascripts %>','<%= dirs.public.stylesheets %>', '<%= dirs.public.images %>'],
            dist: ['<%= dirs.public.root %>/dist/', 'conf/assets.map']
        },

        copy: {
            jsVendor: {
                cwd: '<%= dirs.assets.javascripts %>/vendor',
                src: ['**'],
                dest: '<%= dirs.public.javascripts %>/vendor',
                expand: true
            },
            images: {
                cwd: '<%= dirs.assets.images %>',
                src: ['**'],
                dest: '<%= dirs.public.images %>',
                expand: true
            }
        },

        asset_hash: {
            options: {
                preserveSourceMaps: true,
                assetMap: 'conf/assets.map',
                hashLength: 8,
                algorithm: 'md5',
                srcBasePath: 'public/',
                destBasePath: 'public/',
                references: [
                    '<%= dirs.public.root %>/dist/stylesheets/**/*.css'
                ]
            },
            staticfiles: {
                files: [{
                    src: [
                        '<%= dirs.public.stylesheets %>/**/*.css',
                        '<%= dirs.public.javascripts %>/**/*.js',
                        '<%= dirs.public.javascripts %>/**/*.map',
                        '<%= dirs.public.images %>/**/*'
                    ],
                    dest: '<%= dirs.public.root %>/dist/'
                }]
            }
        },
        sass: {
            options: {
                sourceMap: true,
                outputStyle: 'compressed'
            },
            dist: {
                files: {
                    '<%= dirs.public.stylesheets %>/main.min.css': '<%= dirs.assets.stylesheets %>/main.scss',
                    '<%= dirs.public.stylesheets %>/ie9.min.css': '<%= dirs.assets.stylesheets %>/ie9.scss',
                    '<%= dirs.public.stylesheets %>/ie-old.min.css': '<%= dirs.assets.stylesheets %>/ie-old.scss'
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

    grunt.registerTask('compile', function(){
        grunt.task.run([
            'clean:assets',
            'clean:dist',
            'sass',
            'requirejs',
            'copy:images',
            'copy:jsVendor',
            'asset_hash',
            'clean:assets'
        ]);
    });

    grunt.registerTask('default', ['compile']);
};
