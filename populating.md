---
title: Populating the database
---
  
It is recommended to treat the SOS as a black box appliance and use the InsertSensor/InsertObservation methods to
populate the SOS with sensor and observations. This approach is favored because it doesn't require knowledge of 
the SOS database schema to set up; the format of these requests won't change (whereas the database tables
may change in future i52n SOS versions.

## [sos-injector-example](https://github.com/ioos/sos-injector-example)

The **[sos-injector-example](https://github.com/ioos/sos-injector-example)** project provides an example usage
of the sos-injector project and a simple way to demonstrate harvesting sensor data from the web and storing
it in your i52n-sos. It pulls data from a single web-based sensor feed and inserts it into a target i52n-sos
at the URL you provide.

## [sos-injector-db](https://github.com/axiomalaska/sos-injector-db)

The **[sos-injector-db](https://github.com/axiomalaska/sos-injector-db)** application queries sensor and observation
data from an arbitrary database and uses the sos-injector project (see below) to inject the data into an i52n SOS.
The configuration and queries used to retrieve the data are stored externally to the source code,
so users should be able to download the release distribution and run the compiled jar after creating
the necessary files.

## [sos-injector](https://github.com/ioos/sos-injector)

The **[sos-injector](https://github.com/ioos/sos-injector)** project is a Maven enabled Java toolkit
that can be used to enter sensor data into an i52n SOS. You can use it to build simple applications to inject
sensor data from an existing database, web feeds, or any other Java accessible source. The
**[sos-injector-example](https://github.com/axiomalaska/sos-injector-example)** shows a simple usage of this project.

## [sensor-web-harvester](https://github.com/ioos/sensor-web-harvester)

The **[sensor-web-harvester](https://github.com/ioos/sensor-web-harvester)** project is a Scala project
showing another usage of the **[sos-injector](https://github.com/axiomalaska/sos-injector)** toolkit.
It harvests sensor information from a variety of web sources and injects the data into an i52n SOS.
