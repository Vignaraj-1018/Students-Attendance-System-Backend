# Use the official OpenJDK image as a base image
FROM openjdk:11-jre-slim

# Set the working directory in the container
WORKDIR /app

# Copy the application JAR file into the container
COPY build/libs/your-app.jar your-app.jar

# Expose the port that the app will run on
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "your-app.jar"]
