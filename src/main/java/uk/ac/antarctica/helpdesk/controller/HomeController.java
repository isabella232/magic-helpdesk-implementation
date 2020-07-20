/*
 * Home page controller
 */
package uk.ac.antarctica.helpdesk.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletException;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

@Controller
public class HomeController {

    @Autowired
    Environment env;

  /**
   * Render top level page (this may need to be changed for different servers)
   * 
   * @param     request
   * @param     model
   * @return
   * @throws    ServletException
   * @throws    IOException
   */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String topLevel(HttpServletRequest request, ModelMap model) throws ServletException, IOException {
        return("home");
    }

  /**
   * Render home page
   * 
   * @param     request
   * @return
   * @throws    ServletException
   * @throws    IOException
   */
    @RequestMapping(value = "/home", method = RequestMethod.GET)
    public String home(HttpServletRequest request) throws ServletException, IOException {
        return("home");
    }

  /**
   * Render home page (debug version)
   * 
   * @param     request
   * @param     model
   * @return
   * @throws    ServletException
   * @throws    IOException
   */
    @RequestMapping(value = "/homed", method = RequestMethod.GET)
    public String homeDebug(HttpServletRequest request, ModelMap model) throws ServletException, IOException {
        model.addAttribute("debug", true);
        return("home");
    }
    
  /**
   * Render thank you page
   * 
   * @param     request
   * @return
   * @throws    ServletException
   * @throws    IOException
   */
    @RequestMapping(value = "/thankyou", method = RequestMethod.GET)
    public String thankyou(HttpServletRequest request) throws ServletException, IOException {
        return("thankyou");
    }

}
