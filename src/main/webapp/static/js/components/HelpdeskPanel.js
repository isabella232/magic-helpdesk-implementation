/* Application top-level for MAGIC helpdesk */

magic.classes.HelpdeskPanel = function() {
        
    this.panels = {
        "details": new magic.classes.DetailsPanel(),        
        "other": new magic.classes.OtherInformationPanel(),
        "aoi": new magic.classes.AoiPanel()
    };
    
    /* Submit request button handler */
    jQuery("#helpdesk-form-go").click(this.submitRequest.bind(this));
    
};

magic.classes.HelpdeskPanel.prototype.validate = function() {
    return(
        this.panels.details.validate() &&
        this.panels.aoi.validate() &&
        this.panels.other.validate()
    );    
};

magic.classes.HelpdeskPanel.prototype.getValue = function() {
    return(jQuery.extend({}, 
        this.panels.details.getValue(),
        this.panels.aoi.getValue(),
        this.panels.other.getValue()
    ));    
};

magic.classes.HelpdeskPanel.prototype.submitRequest = function() {
    //magic.runtime.showAlertModal("Not yet implemented", "error");
    if (this.validate()) {
        /* Submit the request now */
        console.log("Submitting request...");
        var submission = this.getValue();
        var uploadDz = this.panels.other.getPopulatedDropzone();
        if (uploadDz != null) {
            /* Submission gies through dropzone sendingmultiple event handler */
            console.log("Attachment(s) submitted for upload - go through dropzone 'sendingmultiple' event handler");
            uploadDz.on("sendingmultiple", function(data, xhr, formData) {
                jQuery.each(submission, function(k, v) {
                    /* Add key/value pair to dropzone submission */
                    console.log("Appending " + k + " : " + v);
                    formData.append(k, v);                    			
                });
            });
            uploadDz.processQueue();
        } else {
            /* Straight AJAX request submission */
            console.log("No uploads required : do as straight AJAX");
            jQuery.ajax({
                url: magic.config.paths.baseurl + "/add_issue", 
                data: JSON.stringify(submission), 
                method: "POST",
                dataType: "json",
                contentType: "application/json"
            })
            .done(function() {
                window.location = magic.config.paths.baseurl + "/thankyou";
            })
            .fail(function (xhr) {
                magic.runtime.showAlertModal("Request submission failed - details: " + JSON.parse(xhr.responseText)["detail"], "error");
            });    
        }
    } else {
        /* Report user errors */
        magic.runtime.showAlertModal("Please correct any errors indicated and try again. In particular, have you remembered to specify your area of interest?", "error");
    }
};