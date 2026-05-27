# Step 1: Use an official JDK 17 runtime base image
FROM eclipse-temurin:17-jre

# Step 2: Set target work directory
WORKDIR /app

# Step 3: Copy the compiled jar from your target directory into the container
COPY target/*.jar app.jar

# Step 4: Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]