module.exports = function (grunt) {
    grunt.initConfig({
        clean: {
            contents: ['src/main/webapp/static/buildjs/*', 'src/main/webapp/static/buildcss/*']
        },
        concat: {
            helpdeskjs: {
                files: [
                    {        
                        src: [
                            'src/main/webapp/static/js/init.js', 
                            'src/main/webapp/static/js/components/*.js'
                        ],
                        dest: 'src/main/webapp/static/buildjs/helpdesk.js'
                    }
                ]
            },
            helpdeskcss: {
                files: [
                    {
                        src: ['src/main/webapp/static/css/nav.css', 'src/main/webapp/static/css/helpdesk.css'],
                        dest: 'src/main/webapp/static/buildcss/helpdesk.css'
                    }
                ]
            }
        },
        uglify: {
            helpdesk: {
                files: [
                    {        
                        src: 'src/main/webapp/static/buildjs/helpdesk.js',
                        dest: 'src/main/webapp/static/dist/helpdesk.min.js'
                    }
                ]
            }
        },
        cssmin: {
            helpdesk: {
                files: [
                    {        
                        src: 'src/main/webapp/static/buildcss/helpdesk.css',
                        dest: 'src/main/webapp/static/dist/helpdesk.min.css'
                    }
                ]
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-css');
    grunt.registerTask('default', ['clean', 'concat', 'uglify', 'cssmin', 'clean']);
};