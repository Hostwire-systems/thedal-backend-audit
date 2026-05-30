# FROM openjdk:21-jdk-slim as build
# WORKDIR /app

# # Copy entire project structure
# COPY ./thedal-app .

# # Install Maven and build
# RUN apt-get update && apt-get install -y maven
# RUN mvn clean package -DskipTests

# # Runtime stage
# FROM openjdk:21-jdk-slim
# WORKDIR /app

# COPY --from=build /app/target/*.jar app.jar
# EXPOSE 8080
# ENTRYPOINT ["java","-jar","app.jar"]


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