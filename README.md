# MAGIC Helpdesk Request System

Allows BAS users to submit requests to the MAGIC helpdesk via a web form, specifying area of interest in a map-based way and permitting user attachments.

## Overview

* Updated June 2018, this is the master version of the MAGIC Helpdesk Request System.  It uses JavaScript/HTML/CSS front end, with a Spring Boot Java 8 back-end. The project
is maintained by [David Herbert](mailto:darb1@bas.ac.uk).

## Setup

The MAGIC Helpdesk Request UI is a Java back-end application running within a Tomcat container.  It is deployed on the Virtual Machine (VM) bslmaga and accessible through the endpoints:

<pre>
http://bslmaga.nerc-bas.ac.uk/helpdesk
https://geo.web.bas.ac.uk/helpdesk
</pre>

The latter is the general endpoint available outside of BAS.

The source code for the Helpdesk application lives in the repository of this project.

## Deployment

There are two main ways to build and deploy a new version of the code.  Make your code changes in GitLab, either by editing directly in the GitLab GUI or via another editor and commit/push cycle to GitLab.

### Automated approach via CI Pipeline in Jenkins (bsl-mapengine development server)

Decide whether or not you want to deploy the new version of the code to bslmaga AND bsl-mapengine, or just bsl-mapengine.  Edit the 'deployto.txt' file at the top level of the project.  To deploy to both servers, the content of this file should be the single line 'bslmaga' (without quotes).  To deploy only to dev (e.g. to test a new version without upsetting the live production version), the file should contain just the line '#bslmaga' (i.e. commented out).  Make sure this change is committed and pushed to the master branch in GitLab.

Log into Jenkins at:

<pre>
http://bsl-mapengine.nerc-bas.ac.uk:8084
</pre>

Username 'admin', password as for ADD Geoserver administrator.

You should see the main project dashboard.  Select 'magic_helpdesk', and on the resulting page, select 'Build now' from the lh pane.  The build and deploy cycle will start, the number listed with a bullet graphic and the build number under 'Build History' in the lh pane.  Drop down the menu next to the bullet and select 'Console output' to see the results of the deployment.  If something fails it should be reasonably obvious what has happened.  Failures tend to either be:

1. Syntax errors in JavaScript causing grunt minification of the scripts to fail
2. Tomcat deploy problems

For the former, best to look at the changes you just made, take a look in an editor that validates JavaScript, or use JSLint or another tool.  For the latter, log into the relevant server (here either bslmaga or bsl-mapengine) and check the server catalina.out log, which should give the details.  Ordinarily the build should sign off with 'BUILD SUCCESS'.  Whereupon the new version of the code should be live, give or take an Empty Cache and Hard Reload to get rid of browser cached versions of the old state.

### Manual approach

If you really must do this, see the documentation at:

https://gitlab.data.bas.ac.uk/MAGIC/mapengine/wikis/building-mapengine-manually

Start from 'Clone the repository'.  The repo in this case will be:

https://gitlab.data.bas.ac.uk/MAGIC/general-and-helpdesk.git

The goal is to end up with a helpdesk.war file which can be deployed in bslmaga/bsl-mapengine Tomcat.

## Usage

The GUI itself should be very intuitive and easy-to-use.  All the fields to be filled in have informative tooltips.  Since the new version was launched in 2018 there have been very few problems with usability.  Most of the difficulties have been caused by the interaction with GitLab going wrong.  It may occasionally be necessary to troubleshoot this side of things.

### Troubleshooting the GitLab interaction

Here is an example of a snippet from the server Tomcat catalina.out file (/packages/tomcat/current/logs/catalina.out) where the request to GitLab was successful:

<pre>
===== PUT request to GitLab at https://gitlab.data.bas.ac.uk/api/v4/projects/462/issues =====
Description : Request by <strong>David Herbert</strong> on <strong>24-09-2018</strong><br><strong>Email</strong> : darb1@bas.ac.uk<br><strong>Phone</strong> : 01223221357<br><strong>Required by</strong> : 28-09-2018<br><strong>Details</strong> :<br>This is a test - GitLab was returning an error for submissions 24/09/2018.<br><br><strong>Attached files:</strong><br>Download user attachment : <a href="http://geo.web.bas.ac.uk/helpdesk/issue_attachment?iid=96&f=aoi.zip&mime=application%2Fzip">aoi.zip (713 bytes)</a><br>Download user attachment : <a href="http://geo.web.bas.ac.uk/helpdesk/issue_attachment?iid=96&f=img_0639.jpg&mime=image%2Fjpeg">img_0639.jpg (879.0KB)</a><br>
===== GitLab PUT succeeded =====
===== End of GitLab PUT success message =====
===== POST request to GitLab at https://gitlab.data.bas.ac.uk/api/v4/projects/462/issues =====
===== GitLab POST succeeded =====
===== End of GitLab POST success message =====
</pre>

And a corresponding example where it failed:

<pre>
PUT request to GitLab at https://gitlab.data.bas.ac.uk/api/v4/projects/462/issues =====
===== GitLab PUT failed =====
Status code : 500 returned by GitLab
Content return from GitLab follows:
===== End of GitLab failure message =====
</pre>

The log file can be followed in a blow-by-blow fashion using:

<pre>
tail -f /packages/tomcat/current/logs/catalina.out
</pre>

when logged into the server via the Unix command line.

Use of Chrome debug tools is recommended when trying to see exactly what the GUI demanded of GitLab.  Select 'Network' and 'XHR' to see what happened and any output.  It may be possible to e.g. construct a PostMan request to isolate the GitLab call and test it out of the UI context.

## Developing

The checked out repository can be edited in a number of ways, e.g. through the GitLab editor/IDE, desktop programs like VS Code or Netbeans IDE (https://netbeans.org/).  The project uses Maven to build in the Java dependencies.  Minified versions of the
JavaScript and CSS files are created by running the Gruntfile.js at the top level of the project.

### Version control

This project uses version control. The project repository is located at:
[https://gitlab.data.bas.ac.uk/MAGIC/general-and-helpdesk/tree/master](https://gitlab.data.bas.ac.uk/MAGIC/general-and-helpdesk/tree/master).

Write access to this repository is restricted. Contact the project maintainer [David Herbert](mailto:darb1@bas.ac.uk) to request access.

### Tests

This project uses manual testing only.

## Feedback

The maintainer of this project is [David Herbert - BAS Mapping and Geographic Information Centre](mailto:darb1@bas.ac.uk).

## License

© UK Research and Innovation (UKRI), 2019 - 2020, British Antarctic Survey.

You may use and re-use this software and associated documentation files free of charge in any format or medium, under
the terms of the Open Government Licence v3.0.

You may obtain a copy of the Open Government Licence at http://www.nationalarchives.gov.uk/doc/open-government-licence/
