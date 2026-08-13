# Stage 1: Build the Spring Boot application
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven configuration
COPY pom.xml ./

# Download dependencies
RUN mvn -B -DskipTests dependency:go-offline

# Copy application source
COPY src ./src

# Build the application
RUN mvn -B -DskipTests clean package

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Copy the generated JAR
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]