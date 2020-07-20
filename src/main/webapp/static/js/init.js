/* Global "magic" namespace */

var magic = {
    
    /* Common quantities */
    common: {
    },
    
    /* Layer and view configuration */
    config: {
        paths: {
            baseurl: (window.location.origin || (window.location.protocol + "//" + window.location.hostname + (window.location.port ? ":" + window.location.port: ""))) + "/helpdesk",
            cdn: "https://cdn.web.bas.ac.uk/magic"
        }
    },        
    
    /* Instantiable classes */
    classes: {        
    },
    
    /* Runtime objects */
    runtime: {
        pingSession: function() {
            jQuery.get(magic.config.paths.baseurl + "/ping");
        },
        /**
         * Show a bootbox alert
         * @param {String} message
         * @param {String} type info|warning|error
         */
        showAlertModal: function(message, type) {
            message = message || "An unspecified error occurred";
            type = type || "error";
            var alertClass = type, divStyle = "margin-bottom:0";
            if (type == "error") {
                alertClass = "danger";
                divStyle = "margin-top:10px";
            }
            bootbox.hideAll();
            bootbox.alert(
                '<div class="alert alert-' + alertClass + '" style="' + divStyle + '">' + 
                    (type == "error" ? '<p>An error occurred - more details below:</p>' : '') + 
                    '<p>' + message + '</p>' + 
                '</div>'
            );
        }

    }
    
};

/* Activate session keepalive by application request every 50 mins */
setInterval("magic.runtime.pingSession()", 50*60*1000);


