# IOOS 52°North SOS

[![Master Build Status](https://travis-ci.org/ioos/i52n-sos.svg?branch=master)](https://travis-ci.org/ioos/i52n-sos)

This repository holds the IOOS custom modules for the 
[52°North Sensor Observation System](https://github.com/52North/SOS).

See the project page at <http://ioos.github.io/i52n-sos> for user documentation.

## Docker

These examples require Docker 1.9.0 or higher. Older versions will work with adjustments
left to the user (don't use named volume).

Please review the [Docker documentation](https://docs.docker.com/engine/reference/run/)
if you aren't familiar with Docker.

### Run i52n-sos on port 8083 using a pre-built image on the IOOS Docker Hub repository

```shell
docker run -d -p 8083:8080 -v i52n-sos:/srv/i52n-sos \
  --name i52n-sos --restart=unless-stopped ioos/i52n-sos:1.1
```

This will store configuration data in the `i52n-sos` named volume which
will survive removal of and recreation of the `i52n-sos` container.

The i52n-sos main page will be available at http://localhost:8083

The i52n-sos service endpoint will be available at http://localhost:8083/service

### View logs

```shell
docker logs i52n-sos
```

### Stop the running container

```shell
docker stop i52n-sos
```

### Remove the container (if you need to adjust port, etc)

```shell
docker rm i52n-sos
```

### Start the stopped container

```
docker start i52n-sos
```

### Delete the configuration volume

```
docker volume rm i52n-sos
```

### Increase Tomcat memory

First, create a `setenv.sh` file to set the `JAVA_OPTS` used by Tomcat. Example:

```
JAVA_OPTS="-Djava.awt.headless=true -Xmx4G -XX:+UseConcMarkSweepGC"
```

Then, provide this file to Tomcat inside the `i52n-sos` container when running in Docker. Example:

```
docker run -d -p 8083:8080 -v i52n-sos:/srv/i52n-sos \
  -v $(pwd)/setenv.sh:/usr/local/tomcat/bin/setenv.sh
  --name i52n-sos --restart=unless-stopped ioos/i52n-sos:1.1
```

### Run a PostgreSQL/PostGIS server and i52n-sos together in Docker

Copy the [example database init script](docker/init-db.sh.example) to your server,
rename it to `init-db.sh`, and edit the `sos` user's password. Then:

```
docker network create i52n-sos 2>/dev/null

docker run -d --restart=always --net i52n-sos -e POSTGRES_PASSWORD=SOME_PASSWORD -p 5435:5432 \
  -v $(pwd)/init-db.sh:/docker-entrypoint-initdb.d/init-db.sh \
  -v i52n-sos-db:/var/lib/postgresql/data \
  --name i52n-sos-db mdillon/postgis:9.5

docker run -d --restart=always --net i52n-sos -p 8083:8080 -v i52n-sos:/srv/apps/i52n-sos \
  --name i52n-sos --restart=always ioos/i52n-sos:1.1
```

`i52n-sos-db`'s database will be exposed on the host on port `5435`, but it is available
to the i52n-sos container within the `i52n-sos` Docker network on `5432` (the normal PostgreSQL port).

When setting up the SOS datasource in the installer, use `i52n-sos-db` as the database host,
`5432` as the port, `sos` as the username, and the password you set for the `sos` user in `init-db.sh`.

To see logs for the PostgreSQL/PostGIS server:

```shell
docker logs i52n-sos-db
```

Note that this will run PostgreSQL with the default configuration. You will likely
need to adjust memory settings by issuing `ALTER SYSTEM` commands, e.g.

```
ALTER SYSTEM SET shared_buffers = '4096MB';
ALTER SYSTEM SET max_connections = 200;
ALTER SYSTEM SET work_mem = '160MB';
ALTER SYSTEM SET effective_cache_size = '11GB';
ALTER SYSTEM SET maintenance_work_mem = '960MB';
```

And then restarting the container:

```
docker restart i52n-sos-db
```

Alternatively you can customize a `postgres.conf` file and pass it to the container using a volume
at `/var/lib/postgresql/data/postgres.conf`.

### Backup i52n configuration

```shell
docker run --rm -v i52n-sos:/srv/apps/i52n-sos -v $(pwd):/backup debian:jessie \
  tar cvf /backup/i52n-sos-backup.tar /srv/apps/i52n-sos
```

This will backup the i52n-sos configuration to `i52n-sos-backup.tar`. Note that the tarball
will be owned by root.

### Restore i52n configuration

```shell
docker stop i52n-sos 2> /dev/null

docker run --rm -v i52n-sos:/srv/apps/i52n-sos -v $(pwd)/i52n-sos-backup.tar:/tmp/backup.tar \
  debian:jessie bash -c "rm -rf /src/app/i52n-sos/* && tar xvf /tmp/backup.tar --strip 1"

docker start i52n-sos
```

### Backup and restore PostgreSQL/PostGIS database

Data in the PostgreSQL/PostGIS database should back backed up and restored normally,
e.g using `pg_dump` and `pg_restore`.

Note that you will be prompted for the `POSTGRES_PASSWORD` you set when you created the
`i52n-sos-db` container. You can set up a
[`.pgpass`](http://www.postgresql.org/docs/current/static/libpq-pgpass.html) file to
avoid this. If you forgot the Postgres password, try

```shell
docker exec -it i52n-sos-db bash -c "echo \$POSTGRES_PASSWORD"
```

```shell
pg_dump --host localhost --port 5435 --user postgres -Fc sos --file i52n-sos-db.pgbackup

pg_restore --host localhost --port 5435 --user postgres --dbname sos --clean i52n-sos-db.pgbackup
```

If you don't have psql installed or can't connect to `i52n-sos-db` because of version
issues, you can run the same commands inside a Docker container:

```shell
docker run -it --rm --net i52n-sos -v $(pwd):/backup mdillon/postgis:9.5 \
  pg_dump --host i52n-sos-db --port 5432 --user postgres -Fc \
  sos --file /backup/i52n-sos-db.pgbackup

docker run -it --rm --net i52n-sos -v $(pwd)/i52n-sos-db.pgbackup:/tmp/backup.pgbackup \
  mdillon/postgis:9.5 pg_restore --host i52n-sos-db --port 5432 \
  --user postgres --dbname sos --clean /tmp/backup.pgbackup
```
