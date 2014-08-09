---
---

This is the project page for the [IOOS](http://www.ioos.noaa.gov/) customized build of the
[52°North Sensor Observation Service (SOS)](http://52north.org/sos).

i52n-SOS extends the stock upstream [52°North (52n) SOS](https://github.com/52North/SOS) with
IOOS specific encoding formats, test data, and more.

The IOOS custom encoding formats include:

* Enhanced GetCapabilitiesResponse (extra metadata)
* Enhanced SensorML (extra metadata and network/station/sensor hierarchies)
* O&M and SWE (IOOS m1.0 SOS format)
* netCDF (CF 1.6/ACDD 1.1/NODC 1.0/IOOS 1.0 conventions, one sensor per netCDF file, output as a zip file)   

The IOOS test data:

* Generates network, station, and sensor procedures
* Uses IOOS SensorML formats and metadata
* Uses CF standard names for observed properties
* Uses udunits
* Generates time series and time series profile data
* Randomly generates global station locations
* Includes test client example requests matching test data values
 
The project currently aims to conform to the
[IOOS SOS Milestone 1 Standard](https://code.google.com/p/ioostech/source/browse/#svn%2Ftrunk%2Ftemplates%2FMilestone1.0).

## Release Notes

See [Release notes document](https://github.com/ioos/i52n-sos/blob/master/RELEASE-NOTES.txt).

## Mailing List

<i52n@librelist.com> ([Archive](http://librelist.com/browser/i52n/))

## Installation

See [installation instructions](./install.html).

## Upgrading

See [upgrade instructions](./upgrade.html).

## Test Data

To experiment with test data in your SOS, see [test data instructions](./testdata.html).

## Populating

For tools to populate your SOS, see [populating instructions](/populating.html).

## Concepts

For an overview of SOS concepts, see [concepts](/concepts.html).

## Test instances

A test instance of the latest release of i52n-SOS is available:
   
<http://ioossos.axiomalaska.com/52n-sos-ioos-stable>

A test instance of the current development version (bleeding edge/not yet released) of i52n-SOS is also available:

<http://ioossos.axiomalaska.com/52n-sos-ioos-dev>

## Issue Tracker

Report issues on the [GitHub issue tracker](https://github.com/ioos/i52n-sos/issues). 

## Known Limitations

* Quality flags not yet supported
* Profile orientation/pitch/roll static data block not supported in GetObservation output
* Station and sensor location in GetObservation are determined from observation locations
* ResultTemplate operations are not supported
   
## Other resources

### Upstream 52North resources:
  
* [52n SOS GitHub repository](https://github.com/52North/SOS)
* [52n SOS main page](http://52north.org/sos)
* [52n SOS 4.0 Documentation Wiki](https://wiki.52north.org/bin/view/SensorWeb/SensorObservationServiceIVDocumentation)
* [52n SOS 4.0 Develeopment Wiki](https://wiki.52north.org/bin/view/SensorWeb/SensorObservationServiceIV)

### IOOS SOS resources:

* [ioostech Google Code page](http://code.google.com/p/ioostech/)
* [ioostech_dev Google Group](https://groups.google.com/forum/?#!forum/ioostech_dev)
  
Information on OGC SOS standards can be found [here](http://www.opengeospatial.org/standards/sos).
