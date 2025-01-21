FROM openjdk:21-jdk-slim
WORKDIR /app
COPY build/libs/kds-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
