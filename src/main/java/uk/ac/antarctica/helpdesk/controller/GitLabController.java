/*
 * Interact with the GitLab API to add new issues
 */
package uk.ac.antarctica.helpdesk.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import static java.time.temporal.ChronoUnit.DAYS;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import uk.ac.antarctica.helpdesk.util.PackagingUtils;

@Controller
public class GitLabController implements ServletContextAware {

    private static final DateTimeFormatter UK_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Autowired
    private Environment env;

    @Autowired
    private JdbcTemplate magicTpl;

    ServletContext context;

    /**
     * Submit the data payload as a new issue to the GitLab API Payload
     * contains:
     *
     * ====== Details panel ======
     * helpdesk-details-name : name of submitter -
     * helpdesk-details-email : email address of submitter -
     * helpdesk-details-phone : phone number of submitter -
     * helpdesk-details-requiredBy : the due date for the request to be completed (yyyy-MM-dd)
     *
     * ====== AOI panel ======
     * helpdesk-aoi-area : WKT string, or array of these, polygons to be converted to a shapefile (if not empty)
     * helpdesk-aoi-description : description of the AOI - added to GitLab description
     *
     * ====== Other information panel ======
     * helpdesk-other-details : additional description of the request
     *
     * There may be one or more user attachments in addition -
     * helpdesk-other-attachments[]
     *
     * Required for GitLab API: - title - description - due_date
     *
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/add_issue", method = RequestMethod.POST, consumes = "multipart/form-data", produces = {"application/json"})
    public ResponseEntity<String> addGitLabIssue(MultipartHttpServletRequest request) throws Exception {

        ResponseEntity<String> ret;

        /*
         * Dump POST parameters
         */
        System.out.println("*** Received POST parameters ***");
        Enumeration parmNames = request.getParameterNames();
        while (parmNames.hasMoreElements()) {
            String key = (String) parmNames.nextElement();
            System.out.println(key + " : >" + request.getParameter(key) + "<");
        }
        System.out.println("*** End of POST parameters ***");

        /*
         * Dump file attachments
         */
        System.out.println("*** File attachments ***");
        StringBuilder sb = new StringBuilder();
        Map<String, MultipartFile> fmp = request.getFileMap();
        if (fmp == null || fmp.isEmpty()) {
            System.out.println("None uploaded");
        } else {
            int count = 1;
            for (MultipartFile mpf : request.getFileMap().values()) {
                sb.append("=== File no ").append(count).append("\n");
                sb.append("File name : ").append(mpf.getOriginalFilename()).append("\n");
                sb.append("Name : ").append(mpf.getName()).append("\n");
                sb.append("Content type : ").append(mpf.getContentType()).append("\n");
                sb.append("Size : ").append(mpf.getSize()).append("\n");
                sb.append("=== End of file no ").append(count).append("\n");
                count++;
            }
        }
        String attachmentInfo = sb.toString();
        System.out.println(attachmentInfo);
        System.out.println("*** End of file attachments ***");

        /*
         * Validate input
         */
        String name = request.getParameter(env.getProperty("magic.helpdesk.param-prefix") + "details-name");
        String phone = request.getParameter(env.getProperty("magic.helpdesk.param-prefix") + "details-phone");
        String email = request.getParameter(env.getProperty("magic.helpdesk.param-prefix") + "details-email");
        String requiredBy = request.getParameter(env.getProperty("magic.helpdesk.param-prefix") + "details-requiredby");
        String[] aoi = retrieveAois(request.getParameter(env.getProperty("magic.helpdesk.param-prefix") + "aoi-area"));
        String aoidesc = request.getParameter(env.getProperty("magic.helpdesk.param-prefix") + "aoi-description");
        String details = request.getParameter(env.getProperty("magic.helpdesk.param-prefix") + "other-details");

        String errMsg = validateInput(name, email, requiredBy, details);
        if (errMsg == null) {
            LocalDate now = LocalDate.now();
            String title = name + " on " + now.format(UK_DATE);
            String description = assembleDescription(name, phone, email, requiredBy, aoi, aoidesc, details);
            String created = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            String labels = getLabelsForIssue(requiredBy);
            int issueId = postToGitLab(title, "No files available", requiredBy, created, labels);
            if (issueId == -1) {
                ret = PackagingUtils.packageResults(HttpStatus.INTERNAL_SERVER_ERROR, null, "Failed to submit issue to GitLab - check logs for details");
            } else {
                ArrayList<AttachmentData> attachmentProperties = new ArrayList();
                /*
                 * Create shapefile from any WKT supplied
                 */
                if (createAoiShapefile(attachmentProperties, aoi, issueId)) {
                    /*
                     * Move any attachments appropriately now we have an id
                     */
                    fileAttachments(attachmentProperties, request.getFileMap(), issueId);
                    description = addAttachmentInfo(request.getRequestURL().toString(), attachmentProperties, issueId, description);
                    if (putToGitLab(issueId, description)) {
                        ret = PackagingUtils.packageResults(HttpStatus.OK, null, "Ok");
                    } else {
                        ret = PackagingUtils.packageResults(HttpStatus.INTERNAL_SERVER_ERROR, null, "Failed to update description of issue " + issueId);
                    }
                } else {
                    ret = PackagingUtils.packageResults(HttpStatus.INTERNAL_SERVER_ERROR, null, "Failed to convert your area of interest to a shapefile");
                }
            }
        } else {
            ret = PackagingUtils.packageResults(HttpStatus.BAD_REQUEST, null, errMsg);
        }
        return (ret);
    }

    /**
     * As above, but does not work with multipart form (i.e. no file uploads)
     *
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/add_issue", method = RequestMethod.POST, consumes = "application/json", produces = {"application/json"})
    public ResponseEntity<String> addGitLabIssue(HttpServletRequest request, @RequestBody String payload) throws Exception {
        ResponseEntity<String> ret;

        JsonObject jsonPayload = new JsonParser().parse(payload).getAsJsonObject();

        /*
         * Dump POST parameters
         */
        System.out.println("*** Received POST parameters ***");
        jsonPayload.entrySet().forEach((Map.Entry<String, JsonElement> en) -> {
            String value;
            JsonElement jval = en.getValue();
            if (jval == null || jval.isJsonNull()) {
                value = "";
            } else if (jval.isJsonArray()) {
                JsonArray ja = jval.getAsJsonArray();
                StringBuilder vsb = new StringBuilder();
                for (int i = 0; i < ja.size(); i++) {
                    vsb.append(ja.get(i).getAsString()).append("\n");
                }
                value = vsb.toString();
            } else {
                value = jval.getAsString();
            }
            System.out.println(en.getKey() + " : >" + value + "<");
        });
        System.out.println("*** End of POST parameters ***");

        /*
         * Validate input
         */
        String name = jsonPayload.get(env.getProperty("magic.helpdesk.param-prefix") + "details-name").getAsString();
        String phone = jsonPayload.get(env.getProperty("magic.helpdesk.param-prefix") + "details-phone").getAsString();
        String email = jsonPayload.get(env.getProperty("magic.helpdesk.param-prefix") + "details-email").getAsString();
        String requiredBy = jsonPayload.get(env.getProperty("magic.helpdesk.param-prefix") + "details-requiredby").getAsString();
        String[] aoi = retrieveAois(jsonPayload.get(env.getProperty("magic.helpdesk.param-prefix") + "aoi-area"));
        String aoidesc = jsonPayload.get(env.getProperty("magic.helpdesk.param-prefix") + "aoi-description").getAsString();
        String details = jsonPayload.get(env.getProperty("magic.helpdesk.param-prefix") + "other-details").getAsString();

        String errMsg = validateInput(name, email, requiredBy, details);
        if (errMsg == null) {
            LocalDate now = LocalDate.now();
            String title = name + " on " + now.format(UK_DATE);
            String description = assembleDescription(name, phone, email, requiredBy, aoi, aoidesc, details);
            String created = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            String labels = getLabelsForIssue(requiredBy);
            int issueId = postToGitLab(title, "No files available", requiredBy, created, labels);
            if (issueId == -1) {
                ret = PackagingUtils.packageResults(HttpStatus.INTERNAL_SERVER_ERROR, null, "Failed to submit issue to GitLab - check logs for details");
            } else {
                ArrayList<AttachmentData> attachmentProperties = new ArrayList();
                /*
                 * Create shapefile from any WKT supplied
                 */
                if (createAoiShapefile(attachmentProperties, aoi, issueId)) {
                    description = addAttachmentInfo(request.getRequestURL().toString(), attachmentProperties, issueId, description);
                    if (putToGitLab(issueId, description)) {
                        ret = PackagingUtils.packageResults(HttpStatus.OK, null, "Ok");
                    } else {
                        ret = PackagingUtils.packageResults(HttpStatus.INTERNAL_SERVER_ERROR, null, "Failed to update description of issue " + issueId);
                    }
                } else {
                    ret = PackagingUtils.packageResults(HttpStatus.INTERNAL_SERVER_ERROR, null, "Failed to convert your area of interest to a shapefile");
                }
            }
        } else {
            ret = PackagingUtils.packageResults(HttpStatus.BAD_REQUEST, null, errMsg);
        }
        return (ret);
    }

    /**
     * Download an attachment for a GitLab issue
     *
     * @param request
     * @param response
     * @param iid      issue identifier from GitLab
     * @param f        name of file to download (no prefixed path info)
     * @param mime     mime type
     */
    @RequestMapping(value = "/issue_attachment", method = RequestMethod.GET)
    public void downloader(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(name = "iid", required = true) int iid,
            @RequestParam(name = "f", required = true) String f,
            @RequestParam(name = "mime", required = true) String mime) {

        /*
         * Serves as a validation step to eliminate malicious paths submitted by anyone discovering this endpoint
         */
        String basename = FilenameUtils.getBaseName(f);
        String extension = FilenameUtils.getExtension(f);
        f = basename + (extension != null && !extension.isEmpty() ? "." + extension : "");

        File download = new File(env.getProperty("magic.helpdesk.issues-dir") + "/" + iid + "/" + f);
        if (download.exists() && download.canRead()) {
            try {
                response.setContentType(mime);
                response.addHeader("Content-Disposition", "filename=\"" + f + "\"");
                IOUtils.copyLarge(new FileInputStream(download), response.getOutputStream());
            } catch (IOException ex) {
                System.out.println("Failed to access attachment " + f + " for issue id " + iid + ", error was : " + ex.getMessage());
            }
        } else {
            try {
                String errmsg = "Failed to access attachment " + f + " for issue id " + iid;
                response.setContentType("text/plain");
                response.addHeader("Content-Disposition", "inline; filename=\"error.txt\"");
                IOUtils.copy(new ByteArrayInputStream(errmsg.getBytes()), response.getOutputStream());
            } catch (IOException ex) {
                System.out.println("IIO Exception reporting non-existent attachment " + f + " for issue id " + iid + ", error was : " + ex.getMessage());
            }
        }
    }

    /**
     * Validate an email address
     *
     * @param email
     * @return true if email address is valid
     */
    private boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ae) {
            result = false;
        }
        return (result);
    }

    /**
     * Validate a date in form YYYY-MM-DD
     *
     * @param date
     * @return is the date string supplied is valid yyyy-MM-dd
     */
    private boolean isValidDate(String date) {
        boolean result = true;
        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException dtpe) {
            result = false;
        }
        return (result);
    }

    /**
     * Create a standardised name for a file/table/schema - done by lowercasing,
     * converting all non-alphanumerics to _ and sequences of _ to single _
     *
     * @param name        input file name
     * @param allowDot    allow a single period to delimit the suffix in a filename
     * @param lengthLimit maximum string length, -1 to allow any length
     * @return standard form of name
     */
    private String standardiseName(String name, boolean allowDot, int lengthLimit) {
        String stdName = "";
        if (name != null && !name.isEmpty()) {
            stdName = name.toLowerCase().replaceAll(allowDot ? "[^a-z0-9.]" : "[^a-z0-9]", "_").replaceAll("_{2,}", "_").replaceFirst("_$", "");
            if (allowDot) {
                int lastDot = stdName.lastIndexOf(".");
                stdName = stdName.substring(0, lastDot).replaceAll("\\.", "_") + stdName.substring(lastDot);
            }
            if (Character.isDigit(stdName.charAt(0))) {
                /*
                 * Disallow an initial digit, bad for PostGIS and Geoserver
                 */
                stdName = "x" + stdName;
            }
            if (lengthLimit > 0 && stdName.length() > lengthLimit) {
                stdName = stdName.substring(0, lengthLimit);
            }
        }
        return (stdName);
    }

    /**
     * Validate the set of inputs and return an error message if any fail to check out
     *
     * @param name       submitter's name - must not be null or empty
     * @param email      submitter's email address - must be valid
     * @param requiredBy request due date in form yyyy-MM-dd
     * @param details    request details - must be non-empty
     * @return error message describing invalidity
     */
    private String validateInput(String name, String email, String requiredBy, String details) {
        String msg = null;
        if (name == null || name.isEmpty()) {
            msg = "Invalid name supplied";
        } else if (email == null || !isValidEmailAddress(email)) {
            msg = "Invalid email address supplied";
        } else if (requiredBy == null || !isValidDate(requiredBy)) {
            msg = "Invalid required by date supplied";
        } else if (details == null || details.isEmpty()) {
            msg = "Please supply details of your request";
        }
        return (msg);
    }

    /**
     * Put together a coherent bundle of information to go in the GitLab
     * description field
     *
     * @param name       submitter's name
     * @param phone      phone number/extension
     * @param email      submitter's email address
     * @param requiredBy request due date in form yyyy-MM-dd
     * @param aoi        WKT polygon(s) describing area of interest
     * @param aoidesc    description of area of interest
     * @param details    request details
     * @return description string suitable for GitLab
     */
    private String assembleDescription(String name, String phone, String email, String requiredBy, String[] aoi, String aoidesc, String details) {
        StringBuilder descSb = new StringBuilder();

        descSb.append("Request by <strong>").append(name).append("</strong> on <strong>").append(LocalDate.now().format(UK_DATE)).append("</strong><br>");
        descSb.append("<strong>Email</strong> : ").append(email).append("<br>");
        descSb.append("<strong>Phone</strong> : ").append(phone).append("<br>");
        descSb.append("<strong>Required by</strong> : ").append(LocalDate.parse(requiredBy).format(UK_DATE)).append("<br>");
        if (aoi == null || aoi.length == 0) {
            if (aoidesc != null && !aoidesc.isEmpty()) {
                descSb.append("<strong>AOI description</strong><br> : ").append(aoidesc).append("<br>");
            } else {
                descSb.append("<strong>AOI not applicable</strong>").append("<br>");
            }
        }
        descSb.append("<strong>Details</strong> :<br>").append(details.replaceAll("(\r\n|\n)", "<br>")).append("<br>");
        return (descSb.toString());
    }

    /**
     * Get the labels for the issue
     *
     * @param requiredBy due date in yyyy-MM-dd
     * @return suitable labels for the new issue
     */
    private String getLabelsForIssue(String requiredBy) {
        String labels = env.getProperty("magic.helpdesk.default-labels", "");
        long timeToGo = DAYS.between(LocalDate.now(), LocalDate.parse(requiredBy));
        if (timeToGo <= Long.parseLong(env.getProperty("magic.helpdesk.urgent"))) {
            labels = labels + (!labels.equals("") ? "," : "") + "Urgent";
        }
        return (labels);
    }

    /**
     * Do the POST request to GitLab and return the issue id (or -1 if the submission failed for some reason)
     *
     * @param title       request title
     * @param description request longer description
     * @param requiredBy  due date
     * @param created     creation date at ISO 8601
     * @param labels      issue labels
     * @return id of new GitLab issue
     */
    private int postToGitLab(String title, String description, String requiredBy, String created, String labels) {

        int issueId = -1;
        CloseableHttpClient client = HttpClients.custom().build();

        /*
         * Trace inputs
         */
        System.out.println("===== POST request to GitLab at " + env.getProperty("magic.helpdesk.gitlab-api-url") + " =====");
        System.out.println("Title : " + title);
        System.out.println("Description : " + description);
        System.out.println("Due date : " + requiredBy);
        System.out.println("Created at : " + created);
        System.out.println("Labels : " + labels);
        System.out.println("===== End of trace =====");

        HttpUriRequest request = RequestBuilder.post()
                .setUri(env.getProperty("magic.helpdesk.gitlab-api-url"))
                .setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .setHeader("PRIVATE-TOKEN", env.getProperty("magic.helpdesk.bot-pat"))
                .addParameter("title", title)
                .addParameter("description", description)
                .addParameter("due_date", requiredBy)
                .addParameter("create_at", created)
                .addParameter("labels", labels)
                .build();
        try {
            CloseableHttpResponse response = client.execute(request);
            int status = response.getStatusLine().getStatusCode();
            if (status < 400) {
                /*
                 * Post worked, so attempt to parse the JSON output - example below:
                 * {
                 * "id": 2588,
                 * "iid": 77,
                 * "project_id": 465,
                 * "title": "test issue",
                 * "description": "please delete",
                 * "state": "opened",
                 * "created_at": "2018-07-05T08:55:41.472Z",
                 * "updated_at": "2018-07-05T08:55:41.472Z",
                 * "labels": [
                 * "Bug",
                 * "Normal",
                 * "Open"
                 * ],
                 * "milestone": null,
                 * "author": {
                 * "id": 21,
                 * "name": "Herbert, David J.",
                 * "username": "darb1",
                 * "state": "active",
                 * "avatar_url": "https://secure.gravatar.com/avatar/1ff97033954339ab59c5d6c932881642?s=80&d=identicon",
                 * "web_url": "xxx"
                 * },
                 * "assignee": null,
                 * "user_notes_count": 0,
                 * "upvotes": 0,
                 * "downvotes": 0,
                 * "due_date": "2018-07-31",
                 * "confidential": false,
                 * "web_url": "xxx",
                 * "subscribed": true
                 * }
                 */
                System.out.println("===== GitLab POST succeeded =====");
                try {
                    String content = IOUtils.toString(response.getEntity().getContent());
                    JsonObject glOut = new JsonParser().parse(content).getAsJsonObject();
                    System.out.println("Successfully parsed JSON content output");
                    issueId = glOut.get("iid").getAsInt();
                    System.out.println("Created new issue with id " + issueId);
                } catch (JsonSyntaxException | IOException | UnsupportedOperationException ex) {
                    System.out.println("Failed to parse JSON content output");
                }
                System.out.println("===== End of GitLab POST success message =====");
            } else {
                /*
                 * Failed - log the errors
                 */
                System.out.println("===== GitLab POST failed =====");
                System.out.println("Status code : " + status + " returned by GitLab");
                System.out.println("Content return from GitLab follows:");
                System.out.println(IOUtils.toString(response.getEntity().getContent()));
                System.out.println("===== End of GitLab failure message =====");
            }
        } catch (IOException ex) {
            System.out.println("===== GitLab POST failed with an IO exception =====");
            System.out.println("Details : " + ex.getMessage());
            System.out.println("===== End of GitLab exception report =====");
        }
        return (issueId);
    }

    /**
     * Update the description of an existing GitLab issue
     *
     * @param issueId     GitLab issue identifier
     * @param description full description to write via the API
     * @return true if the PUT succeeded
     */
    private boolean putToGitLab(int issueId, String description) {

        boolean ret = false;

        CloseableHttpClient client = HttpClients.custom().build();

        /*
         * Trace inputs
         */
        System.out.println("===== PUT request to GitLab at " + env.getProperty("magic.helpdesk.gitlab-api-url") + " =====");
        System.out.println("Update description for issue with id : " + issueId);
        System.out.println("Description : " + description);
        System.out.println("===== End of trace =====");

        HttpUriRequest request = RequestBuilder.put()
                .setUri(env.getProperty("magic.helpdesk.gitlab-api-url") + "/" + issueId)
                .setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .setHeader("PRIVATE-TOKEN", env.getProperty("magic.helpdesk.bot-pat"))
                .addParameter("description", description)
                .build();
        try {
            CloseableHttpResponse response = client.execute(request);
            int status = response.getStatusLine().getStatusCode();
            if (status < 400) {
                /*
                 * Put worked, so attempt to parse the JSON output - see method 'postToGitLab()' for details
                 */
                System.out.println("===== GitLab PUT succeeded =====");
                try {
                    String content = IOUtils.toString(response.getEntity().getContent());
                    new JsonParser().parse(content).getAsJsonObject();
                    ret = true;
                    System.out.println("Successfully parsed JSON content output");
                } catch (JsonSyntaxException | IOException | UnsupportedOperationException ex) {
                    System.out.println("Failed to parse JSON content output");
                }
                System.out.println("===== End of GitLab PUT success message =====");
            } else {
                /*
                 * Failed - log the errors
                 */
                System.out.println("===== GitLab PUT failed =====");
                System.out.println("Status code : " + status + " returned by GitLab");
                System.out.println("Content return from GitLab follows:");
                System.out.println(IOUtils.toString(response.getEntity().getContent()));
                System.out.println("===== End of GitLab failure message =====");
            }
        } catch (IOException ex) {
            System.out.println("===== GitLab PUT failed with an IO exception =====");
            System.out.println("Details : " + ex.getMessage());
            System.out.println("===== End of GitLab exception report =====");
        }
        return (ret);
    }

    /**
     * Move any attachments supplied with the request to a specific issue
     * directory
     *
     * @param attachNames base file names of the attachments, standardised
     * @param fileMap     multipart file map containing attachments
     * @param issueId     issue identifier from GitLab
     */
    private void fileAttachments(ArrayList<AttachmentData> attachNames, Map<String, MultipartFile> fileMap, int issueId) {
        String wd = ensureWorkingDirectory(issueId);
        if (wd != null) {
            fileMap.values().forEach((mpf) -> {
                String filename = standardiseName(mpf.getOriginalFilename(), true, -1);
                attachNames.add(new AttachmentData(
                        mpf.getOriginalFilename(),
                        filename,
                        mpf.getContentType(),
                        mpf.getSize()
                ));
                try {
                    mpf.transferTo(new File(wd + "/" + filename));
                    System.out.println("Transferred attachment " + mpf.getOriginalFilename() + " to " + wd + "/" + filename);
                } catch (IOException | IllegalStateException ex) {
                    System.out.println("Failed to transfer attachment " + mpf.getOriginalFilename() + " to " + wd + " - error was " + ex.getMessage());
                }
            });
        }
    }

    /**
     * Add the information about downloading attachments to the existing (supplied) description
     *
     * @param baseUrl
     * @param attachNames
     * @param issueId
     * @param description
     * @return new description
     */
    private String addAttachmentInfo(String baseUrl, ArrayList<AttachmentData> attachNames, int issueId, String description) {
        StringBuilder descSb = new StringBuilder(description);
        descSb.append("<br><strong>Attached files:</strong><br>");
        attachNames.forEach((attach) -> {
            try {
                URIBuilder ub = new URIBuilder(baseUrl.replace("add_issue", "issue_attachment"));
                ub.addParameter("iid", "" + issueId).addParameter("f", attach.getFinalFileName()).addParameter("mime", attach.getContentType());
                if (attach.getFinalFileName().equals("aoi.shp")) {
                    descSb
                            .append("<a href=\"")
                            .append(ub.build().toURL().toString())
                            .append("\">")
                            .append("Download AOI as zipped Shapefile (")
                            .append(sizeFormat(attach.getSize()))
                            .append(")</a><br>");
                } else {
                    descSb
                            .append("Download user attachment : ")
                            .append("<a href=\"")
                            .append(ub.build().toURL().toString())
                            .append("\">")
                            .append(attach.getFinalFileName())
                            .append(" (")
                            .append(sizeFormat(attach.getSize()))
                            .append(")</a><br>");
                }
            } catch (URISyntaxException | MalformedURLException ex) {
                /*
                 * This is never going to happen
                 */
                System.out.println(ex.getMessage());
            }
        });
        return (descSb.toString());
    }

    /**
     * Create an ESRI shapefile from any WKT polygon inputs
     *
     * @param attachNames list of associated files for the issue
     * @param aoi         comma-separated list of WKT POLYGON entities
     * @param issueId     the issue identifier from GitLab
     * @return            true on success, false if something went wrong
     */
    @Transactional
    private boolean createAoiShapefile(ArrayList<AttachmentData> attachNames, String[] aoi, int issueId) {
        if (aoi == null || aoi.length == 0) {
            System.out.println("===== No shapefile to create - AOI not specified =====");
            return(true);
        }
        System.out.println("===== Creating AOI shapefile =====");
        String wd = ensureWorkingDirectory(issueId);
        if (wd != null) {
            /*
             * Create temporary table in PostGIS and populate it
             */
            String tempTable;
            try {
                tempTable = "temp.aoi" + new Date().getTime();
                magicTpl.execute("CREATE TABLE " + tempTable + "(id integer, name varchar, geom geometry(Polygon, 4326))");
                System.out.println("Created temporary table " + tempTable + " =====");
                int id = 1;
                for (String wktPolygon : aoi) {
                    if (!wktPolygon.isEmpty()) {
                        System.out.println("Insert WKT " + wktPolygon + " as record " + id);
                        magicTpl.update("INSERT INTO " + tempTable + " VALUES(?,?,ST_geomFromText(?, 4326))", id, "aoi", wktPolygon);
                        System.out.println("Done");
                        id++;
                    }
                }
            } catch (DataAccessException dae) {
                System.out.println("===== Failed to create AOI shapefile - database problems : " + dae.getMessage() + " =====");
                return(false);
            }
            /*
             * Create shapefile from table
             *
             * Test server configuration
             */
            //ProcessBuilder pb = new ProcessBuilder("/usr/bin/pgsql2shp",
            //        "-h", "localhost", /* Postgres host */
            //        "-p", "5432", /* Port */
            //        "-k", /* Keep identifier case */
            //        "-g", "geom", /* Geometry column */
            //        "-u", "add", /* Database user */
            //        "-P", "a44Pg#!!", /* Password for user */
            //        "-f", wd + "/aoi.shp", /* Shapefile to create */
            //        "magic", /* Database name */
            //        tempTable /* <schema>.<table> to convert */
            //);
            /*
             * Live server configuration (with pgsql2shp - not installed as requires all of postgres/postgis)
             */
            //ProcessBuilder pb = new ProcessBuilder("/usr/bin/pgsql2shp",
            //        "-h", "postgres.nerc-bas.ac.uk", /* Postgres host */
            //        "-p", "5432", /* Port */
            //        "-k", /* Keep identifier case */
            //        "-g", "geom", /* Geometry column */
            //        "-u", "darb1", /* Database user */
            //        "-P", "h2/ddG7x", /* Password for user */
            //        "-f", wd + "/aoi.shp", /* Shapefile to create */
            //        "magic", /* Database name */
            //        tempTable /* <schema>.<table> to convert */
            //);
            /*
             * Live server configuration (with ogr2ogr)
             */
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/ogr2ogr",
                    "-f", "ESRI Shapefile",
                    wd + "/aoi.shp",
                    "PG:host=bslmagd.nerc-bas.ac.uk user=add dbname=magic schemas=temp password=a44Pg#\\!\\!",
                    tempTable
            );
            pb.inheritIO();
            pb.directory(new File(wd));
            pb.redirectErrorStream(true);
            Process p;
            /*
             * Trace program args
             */
            System.out.println("----- ogr2ogr args -----");
            pb.command().forEach((pbarg) -> {
                System.out.println(pbarg);
            });
            System.out.println("----- End of args -----");
            try {
                System.out.println("Starting ogr2ogr...");
                p = pb.start();
                p.waitFor(Long.parseLong(env.getProperty("magic.helpdesk.shp-create-timeout")), TimeUnit.SECONDS);
                /*
                 * Successfully created shapefile - add in ESRI .prj file and zip
                 */
                File workDir = new File(wd);
                FileUtils.copyFileToDirectory(new File(context.getRealPath("/WEB-INF/prj/aoi.prj")), workDir);
                File zipFile = new File(wd + "/aoi.zip");
                try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zipOut = new ZipOutputStream(fos)) {
                    String[] mandatoryShp = new String[]{"aoi.shp", "aoi.shx", "aoi.dbf", "aoi.prj"};
                    for (String sfName : mandatoryShp) {
                        File toZip = new File(wd + "/" + sfName);
                        try (FileInputStream fis = new FileInputStream(toZip)) {
                            ZipEntry zipEntry = new ZipEntry(toZip.getName());
                            zipOut.putNextEntry(zipEntry);
                            byte[] bytes = new byte[1024];
                            int length;
                            while ((length = fis.read(bytes)) >= 0) {
                                zipOut.write(bytes, 0, length);
                            }
                        }
                    }
                }
                attachNames.add(new AttachmentData(
                        null, "aoi.zip", "application/zip", FileUtils.sizeOf(zipFile)
                ));
                System.out.println("Completed");
            } catch (IOException ex) {
                System.out.println("===== Failed to create shapefile from AOI - details : " + ex.getMessage() + " =====");
                return(false);
            } catch (InterruptedException ex) {
                System.out.println("===== Interrupted while creating shapefile from AOI - details : " + ex.getMessage() + " =====");
                return(false);
            }

            /*
             * Remove table
             */
            System.out.println("Deleting temporary table");
            magicTpl.execute("DROP TABLE IF EXISTS " + tempTable + " CASCADE");
        } else {
            System.out.println("===== Failed to create working directory =====");
            return(false);
        }
        System.out.println("===== Create AOI shapefile completed =====");
        return(true);
    }

    /**
     * Construct string array of WKT AOIs when the data was supplied in a request parameter as a string (multipart case)
     *
     * @param aoiData
     * @return
     */
    private String[] retrieveAois(String aoiData) {
        String[] out = null;
        if (aoiData != null && !aoiData.isEmpty()) {
            String[] wktArr = aoiData.replaceAll(",POLYGON", "~POLYGON").split("~");
            out = new String[wktArr.length];
            int count = 0;
            for (String wkt : wktArr) {
                out[count++] = wkt;
            }
        }
        return(out);
    }

    /**
     * Construct string array of WKT AOIs when the data was supplied as a JSON array or a string (non-multipart case)
     *
     * @param aoiData
     * @return
     */
    private String[] retrieveAois(JsonElement aoiData) {
        String[] out = null;
        if (aoiData != null && !aoiData.isJsonNull()) {
            if (aoiData.isJsonArray()) {
                JsonArray ja = aoiData.getAsJsonArray();
                if (ja.size() > 0) {
                    out = new String[ja.size()];
                    for (int i = 0; i < ja.size(); i++) {
                        out[i] = ja.get(i).getAsString();
                    }
                }
            } else {
                out = new String[]{aoiData.getAsString()};
            }
        }
        return(out);
    }

    /**
     * Ensure that the issue-specific working directory exists
     *
     * @param issueId issue identifier
     * @return working directory path
     */
    private String ensureWorkingDirectory(int issueId) {
        String wd = env.getProperty("magic.helpdesk.issues-dir") + "/" + issueId;
        try {
            File wdf = new File(wd);
            if (!wdf.isDirectory()) {
                System.out.println("===== Creating issue working directory " + wd + " =====");
                FileUtils.forceMkdir(wdf);
            } else {
                System.out.println("===== Issue working directory " + wd + " exists =====");
            }
        } catch (IOException ex) {
            System.out.println("==== Failed to create working directory " + wd + " - error was " + ex.getMessage() + " =====");
            wd = null;
        }
        return (wd);
    }

    /**
     * Human readable file size formatter
     *
     * @param filesize size of file in bytes
     * @return formatted value
     */
    private String sizeFormat(long filesize) {
        DecimalFormat df2 = new DecimalFormat(".##");
        if (filesize >= 1073741824) {
            return (df2.format((double) (filesize / 1073741824L)) + "GB");
        } else {
            if (filesize >= 1048576) {
                return (df2.format((double) (filesize / 1048576L)) + "MB");
            } else {
                if (filesize >= 1024) {
                    return (df2.format((double) (filesize / 1024L)) + "KB");
                } else {
                    return (filesize + " bytes");
                }
            }
        }
    }

    @Override
    public void setServletContext(ServletContext sc) {
        context = sc;
    }

    public class AttachmentData {

        private String origFileName;
        private String finalFileName;
        private String contentType;
        private long size;

        public AttachmentData(String mOrigFileName, String mFinalFileName, String mContentType, long mSize) {
            origFileName = mOrigFileName;
            finalFileName = mFinalFileName;
            contentType = mContentType;
            size = mSize;
        }

        public String getOrigFileName() {
            return origFileName;
        }

        public void setOrigFileName(String origFileName) {
            this.origFileName = origFileName;
        }

        public String getFinalFileName() {
            return finalFileName;
        }

        public void setFinalFileName(String finalFileName) {
            this.finalFileName = finalFileName;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

    }

}
