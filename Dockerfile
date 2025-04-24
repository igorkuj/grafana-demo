FROM eclipse-temurin:21-jdk
WORKDIR /app

# Just copy the fat JAR (with resources baked in)
COPY target/*.jar ./app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
