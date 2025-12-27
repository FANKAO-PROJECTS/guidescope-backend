# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Development stage
FROM eclipse-temurin:17-jdk-alpine AS dev
WORKDIR /app
COPY --from=build /app/pom.xml .
COPY --from=build /app/src ./src
# Install Maven to support hot-reload via spring-boot:run
RUN apk add --no-cache maven
ENTRYPOINT ["mvn", "spring-boot:run"]

# Production stage
FROM eclipse-temurin:17-jre-alpine AS prod
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
