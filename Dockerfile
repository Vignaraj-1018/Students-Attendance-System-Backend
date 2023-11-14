# Use the official OpenJDK image as a base image
FROM openjdk:11-jre-slim

# Set the working directory in the container
WORKDIR /app

RUN mvn clean install

# Copy the application JAR file into the container
COPY build/attendance-system-0.0.1-SNAPSHOT.jar attendance-system-0.0.1-SNAPSHOT.jar

# Expose the port that the app will run on
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "attendance-system-0.0.1-SNAPSHOT.jar"]
