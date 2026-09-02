# Stage 1: Build JAR with Maven & OpenJDK 17
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Production Minimal JRE Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as non-root user for enterprise container security
RUN groupadd -r spring && useradd -r -g spring spring \
    && mkdir -p /app/uploads/attachments \
    && chown -R spring:spring /app/uploads
USER spring:spring

COPY --from=builder /app/target/servicedesk-pro-backend-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]