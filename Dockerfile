# Use the official Gradle image to create a build artifact.
# https://hub.docker.com/_/gradle
FROM gradle:jdk17 AS build

# Copy the code into the container
COPY . /home/gradle/src
WORKDIR /home/gradle/src

# Build the project and dependencies
RUN ./gradlew clean build bootJar --no-daemon


# After building run the thing
FROM amazoncorretto:17.0.7-alpine

VOLUME /tmp
COPY build/libs/ultrabet-0.0.1.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]