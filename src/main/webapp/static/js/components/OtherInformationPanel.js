/* Panel class for capturing other request details */

magic.classes.OtherInformationPanel = function() {
        
    this.formGen = new formwidgets.classes.FormGenerator([
        {
            "field": "details",
            "type": "textarea",
            "defval": 
                "What outputs do you require and what is the output to be used for?\n" +  
                "If the request is for a map, how much detail/data would you like shown?\n" + 
                "Approximately what size would you like the map to be?\n" + 
                "Upload any additional attachments below",                
            "title": "Please specify",
            "tooltip": "List any other relevant details of your request here",
            "required": true,
            "height": 10
        },
        {
            "field": "attachments",
            "type": "fileupload",
            "title": "Attachments",
            "url": magic.config.paths.baseurl + "/add_issue",
            "required": false,
            "onsave": function() {
                window.location = magic.config.paths.baseurl + "/thankyou";
            }
        }
    ]);
    this.formGen.markup(jQuery("#helpdesk-other-panel"), "helpdesk-other");
    
    jQuery("#helpdesk-other-details").click(function() {
        this.focus();
        this.select();
    });
    
};

magic.classes.OtherInformationPanel.prototype.getValue = function() {
    return(this.formGen.getValue());
};

magic.classes.OtherInformationPanel.prototype.validate = function() {
    return(this.formGen.validate());
};

magic.classes.OtherInformationPanel.prototype.getPopulatedDropzone = function() {
    return(this.formGen.getFirstPopulatedDropzone());
};