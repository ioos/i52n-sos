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

UPDATE "multilingual_string_settings_values"
SET value = 'IOOS 52N SOS'
WHERE identifier = 'serviceIdentification.title'
AND lang = 'eng';

UPDATE "multilingual_string_settings_values"
SET value = 'IOOS 52North Sensor Observation Service'
WHERE identifier = 'serviceIdentification.abstract'
AND lang = 'eng';

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

INSERT INTO "settings"
SELECT 'ioos.disclaimer'
WHERE NOT EXISTS (
  SELECT identifier
  FROM "settings"
  WHERE identifier = 'ioos.disclaimer'
);

INSERT INTO "string_settings"
SELECT 'Data provided without any guarantee of accuracy. Provider assumes no liability whatsoever. USE AT YOUR OWN RISK.', 'ioos.disclaimer'
WHERE NOT EXISTS (
  SELECT value
  FROM "string_settings"
  WHERE identifier = 'ioos.disclaimer'
);

--ENABLE OPERATIONS
INSERT INTO "operations" (operation, service, version, active) VALUES ('InsertObservation', 'SOS', '2.0.0', 1);
INSERT INTO "operations" (operation, service, version, active) VALUES ('InsertSensor', 'SOS', '2.0.0', 1);
INSERT INTO "operations" (operation, service, version, active) VALUES ('UpdateSensorDescription', 'SOS', '2.0.0', 1);

-- REMOVE ONCE THESE VALUES ARE IN THE UPSTREAM DEFAULT SETTINGS
INSERT INTO "settings"
SELECT 'exi.fidelity.lexical.value'
WHERE NOT EXISTS (
  SELECT identifier
  FROM "settings"
  WHERE identifier = 'exi.fidelity.lexical.value'
);

INSERT INTO "boolean_settings"
SELECT '1', 'exi.fidelity.lexical.value'
WHERE NOT EXISTS (
  SELECT value
  FROM "boolean_settings"
  WHERE identifier = 'exi.fidelity.lexical.value'
);

--CHOICE
INSERT INTO "settings"
SELECT 'exi.fidelity'
WHERE NOT EXISTS (
  SELECT identifier
  FROM "settings"
  WHERE identifier = 'exi.fidelity'
);

INSERT INTO "string_settings"
SELECT 'exi.fidelity.specific', 'exi.fidelity'
WHERE NOT EXISTS (
  SELECT value
  FROM "string_settings"
  WHERE identifier = 'exi.fidelity'
);

INSERT INTO "settings"
SELECT 'exi.fidelity.comments'
WHERE NOT EXISTS (
  SELECT identifier
  FROM "settings"
  WHERE identifier = 'exi.fidelity.comments'
);

INSERT INTO "boolean_settings"
SELECT 0, 'exi.fidelity.comments'
WHERE NOT EXISTS (
  SELECT value
  FROM "boolean_settings"
  WHERE identifier = 'exi.fidelity.comments'
);

--CHOICE
INSERT INTO "settings"
SELECT 'exi.grammar'
WHERE NOT EXISTS (
  SELECT identifier
  FROM "settings"
  WHERE identifier = 'exi.grammar'
);

INSERT INTO "string_settings"
SELECT 'exi.grammar.schemaless', 'exi.grammar'
WHERE NOT EXISTS (
  SELECT value
  FROM "string_settings"
  WHERE identifier = 'exi.grammar'
);

--CHOICE
INSERT INTO "settings"
SELECT 'exi.grammar.schema'
WHERE NOT EXISTS (
  SELECT identifier
  FROM "settings"
  WHERE identifier = 'exi.grammar.schema'
);

INSERT INTO "string_settings"
SELECT 'exi.grammar.schema.sos.20', 'exi.grammar.schema'
WHERE NOT EXISTS (
  SELECT value
  FROM "string_settings"
  WHERE identifier = 'exi.grammar.schema'
);

--CHOICE
INSERT INTO "settings"
SELECT 'exi.alignment'
WHERE NOT EXISTS (
  SELECT identifier
  FROM "settings"
  WHERE identifier = 'exi.alignment'
);

INSERT INTO "string_settings"
SELECT 'BIT_PACKED', 'exi.alignment'
WHERE NOT EXISTS (
  SELECT value
  FROM "string_settings"
  WHERE identifier = 'exi.alignment'
);

INSERT INTO "settings"
SELECT 'exi.fidelity.prefixes'
WHERE NOT EXISTS (
  SELECT identifier
  FROM "settings"
  WHERE identifier = 'exi.fidelity.comment'
);

INSERT INTO "boolean_settings"
SELECT 1, 'exi.fidelity.prefixes'
WHERE NOT EXISTS (
  SELECT value
  FROM "boolean_settings"
  WHERE identifier = 'exi.fidelity.prefixes'
);

INSERT INTO "settings"
SELECT 'exi.fidelity.processing.instructions'
WHERE NOT EXISTS (
  SELECT identifier
  FROM "settings"
  WHERE identifier = 'exi.fidelity.comment'
);

INSERT INTO "boolean_settings"
SELECT 0, 'exi.fidelity.processing.instructions'
WHERE NOT EXISTS (
  SELECT value
  FROM "boolean_settings"
  WHERE identifier = 'exi.fidelity.processing.instructions'
);

INSERT INTO "settings"
SELECT 'exi.fidelity.dtd'
WHERE NOT EXISTS (
  SELECT identifier
  FROM "settings"
  WHERE identifier = 'exi.fidelity.comment'
);

INSERT INTO "boolean_settings"
SELECT 0, 'exi.fidelity.dtd'
WHERE NOT EXISTS (
  SELECT value
  FROM "boolean_settings"
  WHERE identifier = 'exi.fidelity.dtd'
);
