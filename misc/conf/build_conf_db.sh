#!/bin/sh
rm misc/conf/configuration.db
#default_settings.sql is no longer maintained
#https://github.com/52North/SOS/issues/224
#sqlite3 misc/conf/configuration.db < ../SOS/misc/db/default_settings.sql
#use upstream default_settings.db instead
cp ../SOS/misc/conf/default_settings.db misc/conf/configuration.db
sqlite3 misc/conf/configuration.db < misc/conf/override_default_settings.sql
echo "Configuration copied and initialized. Run a maven build (mvn clean install) and Project/Clean the webapp in eclipse."
