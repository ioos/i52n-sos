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

## 1.0.0 to <a name="1.1">1.1</a>

In most cases you should not have to run any manual database queries to upgrade your database
from i52n-sos 1.0.0 to 1.1 format. Follow the General Upgrade Procedure above and
choose to upgrade your database tables. Note that this process can take a long time
if your database is large.

You may not be able to insert test data after upgrading your database. If you encounter
an error trying to insert test data, run the following queries and try again:

```sql
ALTER TABLE featureofinterest DROP COLUMN hibernatediscriminator;
ALTER TABLE observableproperty DROP COLUMN hibernatediscriminator;
```

If you encounter an error that your database user can't access the `spatial_ref_sys` table,
run the following queries:

```sql
ALTER TABLE spatial_ref_sys OWNER TO sos;
ALTER VIEW geometry_columns OWNER TO sos;
```

## Pre-1.0.0 to <a name="1.0.0">1.0.0</a>

__NOTE:__ Be sure to disable any sensor harvesting and external access while installing the SOS, as SOS requests to the server before it is initialized will cause a crash due to [this bug](https://github.com/ioos/i52n-sos/issues/22). Will resolve in future releases.

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

