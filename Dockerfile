FROM openjdk:21-jdk-slim
WORKDIR /app
# Копируем собранный JAR‑файл из каталога target
COPY target/openai-chatgpt-telegram-bot-0.1.0.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]

