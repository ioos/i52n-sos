-- Script to delete all observations before a certain date
-- and clean up unused records (procedures, observableproperties, etc)
-- afterward

-- Shut down the SOS first, and close any existing connections to the database

-- Deleting directly from tables takes a very long time for large databases
-- Instead, save records to keep in temp tables, truncate tables, and then reinsert

-- This script is for i52n-sos 1.0.0

CREATE TABLE tmp_observation AS
SELECT * FROM observation
WHERE phenomenontimestart >= '2016-08-01';

CREATE TABLE tmp_observationhasoffering AS
SELECT * FROM observationhasoffering
WHERE observationid IN (
  SELECT observationid
  FROM tmp_observation
);

CREATE TABLE tmp_numericvalue AS
SELECT * FROM numericvalue
WHERE observationid IN (
  SELECT observationid
  FROM tmp_observation
);

CREATE TABLE tmp_spatialfilteringprofile AS
SELECT * FROM spatialfilteringprofile
WHERE observationid IN (
  SELECT observationid
  FROM tmp_observation
);

TRUNCATE TABLE observation CASCADE;

INSERT INTO observation SELECT * FROM tmp_observation;

INSERT INTO observationhasoffering SELECT * FROM tmp_observationhasoffering;

INSERT INTO numericvalue SELECT * FROM tmp_numericvalue;

INSERT INTO spatialfilteringprofile SELECT * FROM tmp_spatialfilteringprofile;

DROP TABLE tmp_observation;

DROP TABLE tmp_observationhasoffering;

DROP TABLE tmp_numericvalue;

DROP TABLE tmp_spatialfilteringprofile;

-- update first time and value columns in series
UPDATE series a
SET firsttimestamp = x.firsttimestamp
FROM (
  SELECT seriesid, min(phenomenontimestart) as firsttimestamp
  FROM observation
  GROUP BY seriesid
) x
WHERE a.seriesid = x.seriesid;

UPDATE series a
SET firstnumericvalue = c.value
FROM observation b
JOIN numericvalue c
 ON b.observationid = c.observationid
WHERE a.seriesid = b.seriesid
AND a.firsttimestamp = b.phenomenontimestart;

-- clean up data
DELETE FROM series
WHERE seriesid NOT IN (
  SELECT seriesid FROM observation GROUP BY seriesid
);

DELETE FROM observationconstellation
WHERE procedureid IN (
  SELECT procedureid
  FROM procedure 
  WHERE identifier LIKE 'urn:ioos:sensor:%'
  AND procedureid NOT IN (
    SELECT procedureid
    FROM series
    GROUP BY procedureid
  )
);

DELETE FROM validproceduretime
WHERE procedureid IN (
  SELECT procedureid
  FROM procedure
  WHERE identifier LIKE 'urn:ioos:sensor:%'
  AND procedureid NOT IN (
    SELECT procedureid
    FROM series
    GROUP BY procedureid
  )
);

DELETE FROM sensorsystem
WHERE childsensorid IN (
  SELECT procedureid
  FROM procedure
  WHERE identifier LIKE 'urn:ioos:sensor:%'
  AND procedureid NOT IN (
    SELECT procedureid
    FROM series
    GROUP BY procedureid
  )
);

DELETE FROM procedure
WHERE identifier LIKE 'urn:ioos:sensor:%'
AND procedureid NOT IN (
  SELECT procedureid
  FROM series
  GROUP BY procedureid
);

DELETE FROM observationconstellation
WHERE procedureid IN (
  SELECT procedureid FROM procedure WHERE identifier LIKE 'urn:ioos:station:%'
  AND procedureid NOT IN (
    SELECT parentsensorid FROM sensorsystem
));

DELETE FROM validproceduretime
WHERE procedureid IN (
  SELECT procedureid FROM procedure WHERE identifier LIKE 'urn:ioos:station:%'
  AND procedureid NOT IN (
    SELECT parentsensorid FROM sensorsystem
));

DELETE FROM sensorsystem
WHERE childsensorid IN (
  SELECT procedureid
  FROM procedure
  WHERE identifier LIKE 'urn:ioos:station:%'
  AND procedureid NOT IN (
    SELECT parentsensorid FROM sensorsystem
));

DELETE FROM procedure
 WHERE identifier LIKE 'urn:ioos:station:%'
 AND procedureid NOT IN (
 SELECT parentsensorid FROM sensorsystem
);

-- clean up observableproperty
DELETE FROM observationconstellation
WHERE observablepropertyid NOT IN (
  SELECT observablepropertyid
  FROM series
  GROUP BY observablepropertyid
);

DELETE FROM observableproperty
WHERE observablepropertyid NOT IN (
  SELECT observablepropertyid
  FROM series
  GROUP BY observablepropertyid
);

-- clean up offerings
CREATE TABLE tmp_offering AS
SELECT offeringid
FROM observationhasoffering
GROUP BY offeringid;

INSERT INTO tmp_offering
SELECT offeringid
FROM observationconstellation
GROUP BY offeringid;

INSERT INTO tmp_offering
SELECT offeringid
FROM observationconstellation
WHERE offeringid NOT IN (
  SELECT offeringid
  FROM tmp_offering
)
GROUP BY offeringid;

DELETE FROM offeringallowedfeaturetype
WHERE offeringid NOT IN (
  SELECT offeringid FROM tmp_offering
);

DELETE FROM offeringallowedobservationtype
WHERE offeringid NOT IN (
  SELECT offeringid FROM tmp_offering
);

DELETE FROM offering
WHERE offeringid NOT IN (
  SELECT offeringid FROM tmp_offering
);

DROP TABLE tmp_offering;

-- drop featureofinterest
DELETE FROM featureofinterest
WHERE featureofinterestid NOT IN (
  SELECT featureofinterestid
  FROM series
  GROUP BY featureofinterestid
);
