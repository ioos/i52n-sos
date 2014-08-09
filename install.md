---
title: Installation Instructions
---

# Supported databases

* [PostgreSQL 9.1+](http://www.postgresql.org) / [PostGIS](http://postgis.refractions.net) 2.0+ 
* Oracle 9+
* H2 (in memory or file, not for production)  

# Requirements

* Supported database (see above)
* [Java JRE or JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 6+
* Java Application Server (e.g. [Tomcat](http://tomcat.apache.org) 6.0+)

# PostgreSQL Recommendations

It is essential that PostgreSQL be configured for proper external access and adequate memory.
The defaults are **not** sufficient. This configuration lies outside of the scope of this project,
but here are some recommended resources:

* [PostgreSQL configuration guide](http://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server)
* [pg_hba.conf docs](http://www.postgresql.org/docs/9.1/static/auth-pg-hba-conf.html)
* [pgtune](http://pgfoundry.org/projects/pgtune/)
  
Also, bear in mind that processing millions of sensor observation can be very resource intensive,
so your satisfaction with the SOS will be directly correlated to the performance of your server.
Fast disk access, modern processors (quad-core), and 8+ GBs of RAM are recommended. 

## Set up the database

First, you'll want to create a database user and a new database for the SOS.
Next, enable PostGIS on the new database by running this SQL command:
  
    CREATE EXTENSION postgis;
  
# Deploy the war
 
Deploy the [latest WAR release](https://github.com/ioos/i52n-sos/releases/latest)
to your Java application server. If you're using Tomcat, just copy the war to your Tomcat's webapps directory.

# Complete the install wizard

The new SOS is easily configured using the installation wizard. Open the newly deployed applications homepage in a browser
(e.g. {{http://localhost:8080/52n-sos-ioos-${project.version}}}) and complete the wizard.

On the **Datasource configuration** page, choose the following options:
  
* **Datasource:** (if using PostgreSQL choose PostgreSQL/PostGIS, NOT PostgreSQL/PostGIS Core)
* **Transactional Profile:** enabled (default)
* **Spatial Filtering Profile:** enabled (default)
* **Actions - Create tables:** enabled (default), **UNLESS YOU ARE UPGRADING**, in which case choose Update schema.
  
On the **Settings** page, fill in the fields with your deployment specific information. The following fields should typically be set:
* **Service Provider:** All required fields
* **Service Identification:** All required fields
* **Service:**
  * SOS URL (publicly accessible URL of SOS endpoint, e.g. http://yourdomain.org/52n-sos-ioos/sos)
  * Cache Feeder Threads - more threads = better for cache processing, tune for your hardware 
  
Finally, enter a username and password for the SOS' admin pages. After that, you should be all set. 

# Test the application
 
If all went well, you should now be able to test your application.
Its location will vary depending on your settings, but if you're testing locally a likely location is
<http://localhost:8080/52n-sos-ioos-VERSION> (replace version with the version of the release you downloaded).

See the [test data instructions](./testdata.html) for information on how to insert and remove test data from the SOS.

Using the test client, make a
[GetCapabilities](http://localhost:8080/52n-sos-ioos-VERSION/sos/kvp?service=SOS&request=GetCapabilities&AcceptVersions=1.0.0)
request.
  
See other example requests in the [client interface](http://localhost:8080/52n-sos-ioos-VERSION/client).  
