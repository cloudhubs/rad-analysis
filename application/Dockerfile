FROM docker:latest

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

# add a simple script that can auto-detect the appropriate JAVA_HOME value
# based on whether the JDK or only the JRE is installed
RUN { \
        echo '#!/bin/sh'; \
        echo 'set -e'; \
        echo; \
        echo 'dirname "$(dirname "$(readlink -f "$(which javac || which java)")")"'; \
    } > /usr/local/bin/docker-java-home \
    && chmod +x /usr/local/bin/docker-java-home

ENV JAVA_HOME /usr/lib/jvm/java-1.8-openjdk
ENV PATH $PATH:/usr/lib/jvm/java-1.8-openjdk/jre/bin:/usr/lib/jvm/java-1.8-openjdk/bin

ENV JAVA_VERSION 8u111
ENV JAVA_ALPINE_VERSION 8.111.14-r0

RUN set -x && apk add --no-cache openjdk8 && [ "$JAVA_HOME" = "$(docker-java-home)" ]

RUN apk add jq
RUN apk add bash

ADD target/rad-analysis-application-0.0.5.jar rad-analysis.jar
ADD extract.sh .
CMD chmod +x extract.sh

RUN cat extract.sh

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "rad-analysis.jar"]

# docker build -t diptadas/rad-analysis .
# docker push diptadas/rad-analysis
# docker run -ti -p 8080:8080 -v /var/run/docker.sock:/var/run/docker.sock diptadas/rad-analysis

