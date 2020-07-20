/* Panel class for capturing user details */

magic.classes.DetailsPanel = function() {
        
    this.formGen = new formwidgets.classes.FormGenerator([
        {
            "field": "name",
            "type": "text",
            "title": "Your name",
            "tooltip": "Enter your name",
            "required": true,
            "minlength": 5
        },
        {
            "field": "email",
            "type": "text",
            "subtype": "email",
            "title": "Contact email",
            "tooltip": "Enter a valid email address",
            "required": true
        },   
        {
            "field": "phone",
            "type": "text",
            "title": "Phone number",
            "tooltip": "Your phone/extension number (optional)",
            "required": false
        },          
        {
            "field": "requiredby",
            "type": "datetime",
            "title": "Required by",
            "tooltip": "When we need to have completed this task by",
            "required": true,
            "format": "DD/MM/YYYY",
            "viewmode": "days",
            "rangevalidator": function(field) {
                /* Date must be in the future */
                var inputDate = moment(field.val(), this.format).toDate();
                var ok = field.val() == "" || (Date.parse(inputDate) - Date.now() > 0);
                if (!ok) {
                    field[0].setCustomValidity("Required by date must be in the future");
                }
                return(ok);
            }
        }
    ]);
    this.formGen.markup(jQuery("#helpdesk-details-panel"), "helpdesk-details");
    
};

magic.classes.DetailsPanel.prototype.getValue = function() {
    return(this.formGen.getValue());
};

magic.classes.DetailsPanel.prototype.validate = function() {
    return(this.formGen.validate());
};