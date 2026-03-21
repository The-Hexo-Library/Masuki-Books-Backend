# Build Stage - Use Maven with JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
# Ensure the wrapper has permissions
RUN chmod +x mvnw
# Build the application
RUN ./mvnw clean package -DskipTests

# Run Stage - Use JRE 21
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar
# Render's default port
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "app.jar"]