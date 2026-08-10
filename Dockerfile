# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
COPY src/main/resources ./src/main/resources
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV JAVA_OPTS=""
ENV PORT=8081

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
