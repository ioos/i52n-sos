---
title: SOS Concepts
---

Although they are quite lengthy, there really is no substitute for the [OGC SOS standard documents](http://www.opengeospatial.org/standards/sos).
When in doubt, refer to this standard and the standards referenced therein.

See also:

* [OGC SOS overview page](http://www.ogcnetwork.net/SOS)
* [Wikipedia entry on SOS](http://en.wikipedia.org/wiki/Sensor_Observation_Service)
* [52°North SOS wiki page](https://wiki.52north.org/bin/view/SensorWeb/SensorObservationServiceIVDocumentation#Operations)

## Entities

#### feature_of_interest (feature/foi)

Used to represent the object being sampled. Usually this refers to the location
of an observation, but can also be used to represent the location of a platform, sensing device, etc. This location
information can include a z coordinate (height in meters).    

#### observation

A measurement of an observable property by a procedure at a feature_of_interest.  

#### offering

A convenience name for a set of features, observable propertys, and procedures. Offerings have
tended to confuse the SOS standard and should be understood to only be a shortcut to request
data from a certain set of the three main entities (foi, observable property, and procedure).    

#### observable property (phenomenon)

The observable property being measured (ex. air_temperature). For IOOS, the phenomenon
ID should always refer to a [CF standard name URL](http://cfconventions.org/Data/cf-standard-names/27/build/cf-standard-name-table.html) (where available)
or an [IOOS phenomenon URL](http://mmisw.org/ont/ioos/parameter) when a CF name doesn't exist.  

#### procedure

Any process by which data is collected, produced, or organized. For IOOS purposes, this includes
<<networks, stations, and sensors>>. Procedures can related to each other hierarchically; sensors are children of stations,
stations are children of networks. Although there is no schema restriction, common sense dictates that sensors should only
have one parent station, while stations can belong to multiple network groupings.
