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
* netCDF (CF 1.6/ACDD 1.1/NODC 1.0/IOOS 1.0 conventions)
    * application/x-netcdf - single sensor/observed property
    * application/zip; subtype=x-netcdf - zipped, supports multiple sensors

__NOTE__: netCDF encoding formats require the installation of the netCDF4 C library on the server.
For Debian/Ubuntu systems, this is the `libnetcdf-dev` package.

The IOOS test data:

* Generates network, station, and sensor procedures
* Uses IOOS SensorML formats and metadata
* Uses CF standard names for observed properties
* Uses udunits
* Generates time series and time series profile data
* Randomly generates global station locations
* Includes test client example requests matching test data values
 
The project currently aims to conform to the
[IOOS SOS Milestone 1 Standard](http://ioos.github.io/sos-guidelines/).

## Release Notes

See [Release notes document](https://github.com/ioos/i52n-sos/blob/master/RELEASE-NOTES).

## Mailing List

<i52n-sos@googlegroups.com> ([Sign-up and archive](https://groups.google.com/forum/#!forum/i52n-sos))

## Docker

i52n-sos can be run in Docker using prebuilt images available on the IOOS Docker Hub.
See the [i52n-sos Docker Hub page for more details](https://hub.docker.com/r/ioos/i52n-sos/).

## Installation (non-Docker)

See [installation instructions](./install.html).

## Upgrading

See [upgrade instructions](./upgrade.html).

## Test Data

To experiment with test data in your SOS, see [test data instructions](./testdata.html).

## Populating

For tools to populate your SOS, see [populating instructions](./populating.html).

## Concepts

For an overview of SOS concepts, see [concepts](./concepts.html).

## Test instances

A test instance of the latest release of i52n-SOS is available:
   
<http://demo.i52nsos.axiomdatascience.com>

A test instance of the current development version (bleeding edge/not yet released) of i52n-SOS is also available:

<http://dev.demo.i52nsos.axiomdatascience.com>

## Testing the i52n-sos implementation

The i52n-sos implementation of the IOOS SOS specification can be tested using an (incomplete) collection
of CTL tests. See the [ioos-sos-compliance-tests](https://github.com/ioos/ioos-sos-compliance-tests) project.

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

* [IOOS SOS Guidelines](http://ioos.github.io/sos-guidelines/)
* [IOOS General SOS Issues](https://github.com/ioos/ioos-sos-issues-generic/issues)
* [ioostech_dev Google Group](https://groups.google.com/forum/?#!forum/ioostech_dev)
  
Information on OGC SOS standards can be found [here](http://www.opengeospatial.org/standards/sos).
