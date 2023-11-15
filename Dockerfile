# Use the official OpenJDK image as a base image
FROM openjdk:18.0.1-jdk

# Set the working directory in the container
WORKDIR /app

# Copy the application JAR file into the container
COPY build/attendance-system-0.0.1-SNAPSHOT.jar attendance-system-0.0.1-SNAPSHOT.jar

# Expose the port that the app will run on
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "attendance-system-0.0.1-SNAPSHOT.jar"]
