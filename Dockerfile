FROM tomcat:8.0
MAINTAINER Shane St Clair<shane@axds.co>

RUN apt-get update && apt-get install -y --no-install-recommends openjdk-7-jdk libnetcdf-dev \
      && rm -rf /var/lib/apt/lists/*

#Borrowed from maven:3 Dockerfile
ENV MAVEN_VERSION 3.3.9
ENV MAVEN_HOME /usr/share/maven

RUN mkdir -p /usr/share/maven \
  && curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
    | tar -xzC /usr/share/maven --strip-components=1 \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn


#Add pom.xml and resolve dependencies first so we don't download the internet
#on every build (Docker caches these layers if the pom is unchanged)
#NOTE: Docker Hub is still going to download the internet every time,
#      since it uses --no-cache
#NOTE: It's painful to have to manually copy every module's pom, but no clear way around it
#      Also if a pom is missed its dependencies will just be downloaded on every code change
ADD pom.xml /usr/local/src/i52n-sos/pom.xml
ADD api-ioos/pom.xml /usr/local/src/i52n-sos/api-ioos/pom.xml
ADD coding-ioos/pom.xml /usr/local/src/i52n-sos/coding-ioos/pom.xml
ADD coding-ioos-netcdf/pom.xml /usr/local/src/i52n-sos/coding-ioos-netcdf/pom.xml
ADD ioos-controller/pom.xml /usr/local/src/i52n-sos/ioos-controller/pom.xml
ADD password-hasher/pom.xml /usr/local/src/i52n-sos/password-hasher/pom.xml
ADD test-data-ioos/pom.xml /usr/local/src/i52n-sos/test-data-ioos/pom.xml
ADD webapp-ioos/pom.xml /usr/local/src/i52n-sos/webapp-ioos/pom.xml
WORKDIR /usr/local/src/i52n-sos
RUN mvn clean verify --fail-never

#Add the project
ADD . /usr/local/src/i52n-sos

#Build the project
RUN mvn clean package \
    && mkdir -p /srv/apps \
    && mv webapp-ioos/target/i52n-sos /srv/apps/ \
    && rm -rf /usr/local/src/i52n-sos

#Remove maven
RUN rm /usr/bin/mvn && rm -rf /usr/share/maven

#Send logging to console for Docker
ADD docker/logback.xml /srv/apps/i52n-sos/WEB-INF/classes/logback.xml

#change webapps dir to /srv/apps
RUN sed -i -e 's/appBase="webapps"/appBase="\/srv\/apps"/' $CATALINA_HOME/conf/server.xml

#Add sensor user
RUN useradd --system --home-dir=/srv/apps/i52n-sos sensor \
      && chown -R sensor:sensor /srv/apps/i52n-sos \
      && chown -R sensor:sensor $CATALINA_HOME

#Run as sensor user
USER sensor

#Create volume for data persistence
VOLUME /srv/apps/i52n-sos
