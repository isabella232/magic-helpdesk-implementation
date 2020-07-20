/*
 * Custom error pages
 */
package uk.ac.antarctica.helpdesk.controller;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

@Controller
public class CustomErrorController implements ErrorController {

    private static final String ERROR_PATH = "/error";

    @Autowired
    Environment env;    

    @Autowired
    private ErrorAttributes errorAttributes;

    /**
     * Output an error
     */
    @RequestMapping(value = "/error", method = RequestMethod.GET)
    public String error(HttpServletRequest request, ModelMap model) throws Exception {
        
        Map<String, Object> errAttrs = getErrorAttributes(request, true);
        int status = (int)errAttrs.get("status");
        
        System.out.println("===== CustomErrorController.error() method called with attributes...");
        errAttrs.entrySet().stream().forEach(System.out::println);
                
        String header = request.getHeader("X-Requested-With");
        if (header != null && header.equals("XMLHttpRequest")) {
            /* Ajax call => pass through */
            System.out.println("===== Error handler was from Ajax - pass through");
            return(null);
        } else {                       
            model.addAttribute("httpstatus", errAttrs.get("status"))
                 .addAttribute("httperror", errAttrs.get("error"))
                 .addAttribute("httpmessage", errAttrs.get("message"))
                 .addAttribute("timestamp", errAttrs.get("timestamp"))
                 .addAttribute("referrer", request.getHeader("referer"));                 
            System.out.println("--> Model attributes for error page");
            model.entrySet().stream().forEach(System.out::println);
            System.out.println("===== CustomErrorController.error() generating error page with status " + status);
            return("error");                
        }        
    }

    @Override
    public String getErrorPath() {
        return (ERROR_PATH);
    }

    private Map<String, Object> getErrorAttributes(HttpServletRequest request, boolean includeStackTrace) {
        RequestAttributes requestAttributes = new ServletRequestAttributes(request);
        return(errorAttributes.getErrorAttributes(requestAttributes, includeStackTrace));
    }
   
}
