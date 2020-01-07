FROM openjdk:8-jdk-alpine

ARG JAR_FILE=target/sample-one-0.0.5.jar

ADD ${JAR_FILE} rad-sample-one.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "-Dserver.port=8090", "/rad-sample-one.jar"]

# docker build -t diptadas/rad-sample-one .
# docker push diptadas/rad-sample-one
