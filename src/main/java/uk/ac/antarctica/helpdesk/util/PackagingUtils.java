/*
 * Utility methods for packaging controller returns
 */
package uk.ac.antarctica.helpdesk.util;

import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class PackagingUtils {
    
    /**
     * Do the packaging of return values
     * @param status
     * @param data
     * @param message
     * @return 
     */
    public static ResponseEntity<String> packageResults(HttpStatus status, String data, String message) {
        ResponseEntity<String> ret;
        if (status.equals(HttpStatus.OK)) {
            if (data != null) {
                ret = new ResponseEntity<>(data, status);
            } else {
                JsonObject jo = new JsonObject();
                jo.addProperty("status", status.value());
                jo.addProperty("detail", message == null ? "" : message);
                ret = new ResponseEntity<>(jo.toString(), status);
            }
        } else {
            JsonObject jo = new JsonObject();
            jo.addProperty("status", status.value());
            jo.addProperty("detail", message);
            ret = new ResponseEntity<>(jo.toString(), status);
        }
        return (ret);
    }    
    
}
