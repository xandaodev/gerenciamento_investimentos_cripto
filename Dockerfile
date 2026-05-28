# ambiente base - linux
FROM eclipse-temurin:25-jdk-alpine

# autor
LABEL maintainer="Alexandre Vital"

# cria pasta /app
WORKDIR /app


COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]