/* Panel class for capturing AOI details */

magic.classes.AoiPanel = function() {
        
    this.prefix = "helpdesk-aoi";
    
    this.formGen = {
        "map": {
            "antarctica": new formwidgets.classes.FormGenerator([
                {
                    "field": "antarctica",
                    "type": "mapaoi",
                    "required": false,
                    "region": "antarctica",
                    "container": this.prefix + "-map-antarctica",
                    "width": "100%",
                    "height": "80%"
                }
            ]),
            "arctic": new formwidgets.classes.FormGenerator([
                {
                    "field": "arctic",
                    "type": "mapaoi",
                    "required": false,
                    "region": "arctic",
                    "container": this.prefix + "-map-arctic",
                    "width": "100%",
                    "height": "80%"
                }
            ]),
            "southgeorgia": new formwidgets.classes.FormGenerator([
                {
                    "field": "southgeorgia",
                    "type": "mapaoi",
                    "required": false,
                    "region": "southgeorgia",
                    "container": this.prefix + "-map-southgeorgia",
                    "width": "100%",
                    "height": "80%"
                }
            ]),
            "midlatitudes": new formwidgets.classes.FormGenerator([
                {
                    "field": "midlatitudes",
                    "type": "mapaoi",
                    "required": false,
                    "region": "midlatitudes",
                    "container": this.prefix + "-map-midlatitudes",
                    "width": "100%",
                    "height": "80%"
                }
            ])
        },
        "corners": new formwidgets.classes.FormGenerator([
            {
                "field": "corners",
                "type": "boundingbox",
                "title": "Bounding box",
                "required": false,
                "wkt": true
            }
        ]),
        "lat": new formwidgets.classes.FormGenerator([
            {
                "field": "lat",
                "type": "text",
                "title": "Bounding latitude",
                "tooltip": "Bounding latitude, decimal degrees, DD MM SS.SSH or HDD MM.MM",
                "required": false
            }
        ]),
        "description": new formwidgets.classes.FormGenerator([
            {
                "field": "description",
                "type": "textarea",
                "title": "Description",
                "tooltip": "Enter as full a description of the area covered by your request as you can",
                "required": false
            }
        ])
    };
        
    /* Create the default map */
    this.formGen.map.antarctica.markup(jQuery("#" + this.prefix + "-map-antarctica"), "");
    
    /* How the AOI is to be calculated (map|corners|lat|description) */
    this.aoiMethod = "map";
    
    /* Selected map region */
    this.selectedMap = "antarctica";
    
    /* Assign map region tab change handlers */
    jQuery("a[data-toggle='tab']").filter("[href^='#" + this.prefix + "-map']").on("shown.bs.tab", function(evt) {
        var region = evt.target.hash.replace("#" + this.prefix + "-map-", "");
        if (!this.formGen.map[region].isMarkedUp()) {
            this.formGen.map[region].markup(jQuery("#" + this.prefix + "-map-" + region), "");
        }   
        this.selectedMap = region;
        this.formGen.map[region].inputs[0].refresh();
    }.bind(this));
    
    /* Assign AOI method tab change handlers */
    jQuery("a[data-toggle='tab']").filter("[href^='#" + this.prefix + "-method']").on("shown.bs.tab", function(evt) {
        this.aoiMethod = evt.target.hash.replace("#" + this.prefix + "-method-", "");   
        if (this.aoiMethod != "map" && this.aoiMethod != "na" && !this.formGen[this.aoiMethod].isMarkedUp()) {
            this.formGen[this.aoiMethod].markup(jQuery("#" + this.prefix + "-" + this.aoiMethod + "-panel"), this.prefix);
        }   
    }.bind(this));
    
};

magic.classes.AoiPanel.prototype.getValue = function() {
    var value = {};
    switch(this.aoiMethod) {
        case "map":
            var sketchRet = this.formGen.map[this.selectedMap].getValue();
            value[this.prefix + "-area"] = sketchRet[this.selectedMap];          
            break;
        case "corners":
            var extent = this.formGen.corners.getValue()[this.prefix + "-" + this.aoiMethod];
            value[this.prefix + "-area"] = extent == null ? [] : [extent];  
            break;
        case "lat":
            var extent = this.aoiFromLat(this.formGen.lat.getValue());
            value[this.prefix + "-area"] = extent == null ? [] : [extent]; 
            break;       
        default:
            value[this.prefix + "-area"] = [];
            break;            
    }
    var description = jQuery("#" + this.prefix + "-description");
    value[this.prefix + "-description"] = description.length > 0 ? description.val() : "";
    return(value);
};

magic.classes.AoiPanel.prototype.validate = function() {
    var valid = false;
    var value = this.getValue();
    switch(this.aoiMethod) {
        case "map":           
        case "corners":            
        case "lat":            
            valid = value[this.prefix + "-area"].length > 0;
            break;
        case "description":
            valid = value[this.prefix + "-description"] != "";
        default:
            valid = true;
            break;            
    }    
    return(valid);
};

/**
 * Construct an extent from a circumpolar box bounded by the input latitude
 * @return {String} WKT representation of circumpolar bounding box
 */
magic.classes.AoiPanel.prototype.aoiFromLat = function() {
    var ret = null;
    var blat = jQuery("#" + this.prefix + "-lat");
    var ddLat = ol.coordinate.toFloatDD(blat.val(), true);
    var ok = ol.coordinate.isValid([0.0, ddLat]);
    new formwidgets.classes.ValidationStatus(
        blat,
        ok,
        "Coordinate is invalid",
        true
    ).setFeedback();
    if (ok) {
        /* Passed the validation */
        if (ddLat < 0) {
            ret = ol.extent.toWktPolygon([-180.0, -90.0, 180.0, ddLat]);
        } else {
            ret = ol.extent.toWktPolygon([-180.0, ddLat, 180.0, 90.0]);
        }        
    }
    return(ret);
};
