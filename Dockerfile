FROM openjdk:17-jdk

WORKDIR /app

COPY . .

RUN ./gradlew clean shadowJar

CMD ["java", "-jar", "/app/build/libs/TgWeatherBot-1.0-SNAPSHOT.jar"]
