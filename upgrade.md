---
title: Upgrade Instructions
---

# General Upgrade Procedure

* Log into Admin/Settings, press the Export Settings button, and save the exported settings.
* Remove the old deployed SOS war from the application server and deploy the updated SOS WAR.
* Open the SOS address in a browser and choose to start the install process.
* Choose to upload your exported settings.
* Continue with the normal [installation](./install.html) process, but choose
  __Force update existing tables__ in the Actions section at the bottom of the Datasource configuration screen.

# Notes on Specific Version Upgrades

## Pre-1.0.0 to 1.0.0 <a name="1.0.0"></a>

### Update settings (before installation of new version)

Due to [an issue](https://github.com/52North/SOS/issues/231) in the upstream SOS, the exported JSON settings from 
the old SOS version must be updated before being imported into the new SOS.

The title and abstract settings:

    ...
    "serviceIdentification.abstract": "Some SOS installation",
    ...
    "serviceIdentification.title": "My SOS",
    ...

must be alterted to become multilingual settings:

    ...
    "serviceIdentification.abstract": {
      "eng": "Some SOS installation"
    },
    ...
    "serviceIdentification.title": {
      "eng": "My SOS"
    },
    ...

You can manually edit this settings file, or use the script below on a Linux or Mac OS:

    cp settings.json settings.json.bak
    sed -i 's/\("serviceIdentification\.\(title\|abstract\)": \)\(".*"\)/\1{ "eng": \3 }/' settings.json

### Update database column types (after installation of new version)

i52n-SOS 1.0.0 stores numerical observations as floating point numbers instead of fixed decimal numbers.
You must manually run the following statements against your database to change the column types:

    ALTER TABLE public.numericvalue ALTER COLUMN value TYPE double precision;
    ALTER TABLE public.series ALTER COLUMN firstnumericvalue TYPE double precision;
    ALTER TABLE public.series ALTER COLUMN lastnumericvalue TYPE double precision;

