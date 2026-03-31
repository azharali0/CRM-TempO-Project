# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S crm && adduser -S crm -G crm
RUN mkdir -p /var/crm/uploads && chown -R crm:crm /var/crm

COPY --from=build /app/target/*.jar app.jar

USER crm

EXPOSE 8080

ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
