from eclipse-temurin:25.0.3_9-jdk-alpine-3.23
WORKDIR /app
# Ensure the user isn't root for better security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY target/fortunelink-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "app.jar"]