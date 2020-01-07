FROM openjdk:8-jdk-alpine

ARG JAR_FILE=target/sample-two-0.0.5.jar

ADD ${JAR_FILE} rad-sample-two.jar

EXPOSE 8070

ENTRYPOINT ["java", "-jar", "-Dserver.port=8070", "/rad-sample-two.jar"]

# docker build -t diptadas/rad-sample-two .
# docker push diptadas/rad-sample-two
