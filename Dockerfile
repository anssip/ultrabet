# Use the official Gradle image to create a build artifact.
# https://hub.docker.com/_/gradle
FROM gradle:jdk17 AS build

# Copy the code into the container
COPY . /home/gradle/src
WORKDIR /home/gradle/src

# Build the project and dependencies
RUN ./gradlew clean build bootJar --no-daemon || (echo "Gradle build failed!" && exit 1)
RUN ls -la application/build/libs || (echo "Directory listing failed!" && exit 1)

# After building run the thing
FROM amazoncorretto:17.0.7-alpine

VOLUME /tmp
COPY --from=build /home/gradle/src/application/build/libs/ultrabet-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
