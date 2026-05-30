# # Build stage
# FROM maven:3.8.8-eclipse-temurin-21 AS build
# WORKDIR /app
# COPY pom.xml .
# COPY src ./src
# RUN mvn clean package -DskipTests

# # Run stage
# FROM openjdk:21-jdk
# WORKDIR /app
# COPY --from=build /app/target/Thedalapp.jar app.jar
# COPY src/main/resources/ibmcloud-postgres.crt /app/certs/ibmcloud-postgres.crt
# EXPOSE 8080
# ENV SSL_CERT_PATH=/app/certs/ibmcloud-postgres.crt
# ENTRYPOINT ["java","-jar", "/app/app.jar"]

FROM openjdk:21-jdk-slim as build
WORKDIR /app

# Copy entire project structure
COPY ./thedal-app .

# Install Maven and build
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:21-jdk-slim
WORKDIR /app

# Install required font libraries
RUN apt-get update && apt-get install -y \
    fontconfig \
    libfreetype6 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

# Set JVM to run in headless mode
ENV JAVA_TOOL_OPTIONS=-Djava.awt.headless=true

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]