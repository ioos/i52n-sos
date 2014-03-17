-- To build configuration.db: 
-- rm misc/conf/configuration.db
-- sqlite3 misc/conf/configuration.db < ../52n-sos/misc/db/default_settings.sql
-- sqlite3 misc/conf/configuration.db < misc/db/override_default_settings.sql

UPDATE "integer_settings"
SET value = 8
WHERE identifier = 'service.cacheThreadCount';

UPDATE "integer_settings"
SET value = 0
WHERE identifier = 'service.capabilitiesCacheUpdateInterval';

UPDATE "boolean_settings"
SET value = 0
WHERE identifier = 'service.encodeFullChildrenInDescribeSensor';

UPDATE "boolean_settings"
SET value = 1
WHERE identifier = 'procedureDesc.ENRICH_WITH_OFFERINGS';

UPDATE "boolean_settings"
SET value = 0
WHERE identifier = 'procedureDesc.ENRICH_WITH_FEATURES';

UPDATE "boolean_settings"
SET value = 0
WHERE identifier = 'procedureDesc.ENRICH_WITH_DISCOVERY_INFORMATION';

UPDATE "boolean_settings"
SET value = 0
WHERE identifier = 'service.addOutputsToSensorML';

UPDATE "string_settings"
SET value = 'http://www.opengis.net/def/crs/EPSG/0/'
WHERE identifier = 'misc.srsNamePrefixSosV1';

UPDATE "string_settings"
SET value = '1-301-427-2420'
WHERE identifier = 'serviceProvider.phone';

UPDATE "string_settings"
SET value = 'noaa.ioos.webmaster@noaa.gov'
WHERE identifier = 'serviceProvider.email';

UPDATE "string_settings"
SET value = '1100 Wayne Ave., Suite 1225'
WHERE identifier = 'serviceProvider.address';

UPDATE "string_settings"
SET value = 'Silver Spring'
WHERE identifier = 'serviceProvider.city';

UPDATE "string_settings"
SET value = 'MA'
WHERE identifier = 'serviceProvider.state';

UPDATE "string_settings"
SET value = '20910'
WHERE identifier = 'serviceProvider.postalCode';

UPDATE "string_settings"
SET value = 'USA'
WHERE identifier = 'serviceProvider.country';

UPDATE "string_settings"
SET value = 'IOOS 52N SOS'
WHERE identifier = 'serviceIdentification.title';

UPDATE "string_settings"
SET value = 'IOOS 52North Sensor Observation Service'
WHERE identifier = 'serviceIdentification.abstract';

UPDATE "string_settings"
SET value = 'http://opengeospatial.net'
WHERE identifier = 'serviceIdentification.serviceTypeCodeSpace';

UPDATE "string_settings"
SET value = 'IOOS'
WHERE identifier = 'serviceProvider.name';

UPDATE "uri_settings"
SET value = 'http://www.ioos.noaa.gov/'
WHERE identifier = 'serviceProvider.site';

UPDATE "uri_settings"
SET value = 'http://UPDATE_SOS_URL_SETTING'
WHERE identifier = 'service.sosUrl';

INSERT INTO "procedure_encodings" VALUES('text/xml; subtype="sensorML/1.0.1/profiles/ioos_sos/1.0','SOS','1.0.0',1);

UPDATE "procedure_encodings"
SET active = 0
WHERE procedureDescriptionFormat != 'text/xml; subtype="sensorML/1.0.1/profiles/ioos_sos/1.0';

INSERT INTO "observation_encodings" VALUES('text/xml; subtype="om/1.0.0/profiles/ioos_sos/1.0"','SOS','1.0.0',1);