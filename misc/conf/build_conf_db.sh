#!/bin/sh
rm misc/conf/configuration.db
sqlite3 misc/conf/configuration.db < ../52n-sos/misc/db/default_settings.sql
sqlite3 misc/conf/configuration.db < misc/conf/override_default_settings.sql
