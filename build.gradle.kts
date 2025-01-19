plugins {
    id("org.springframework.boot") version "3.0.0"
    id("io.spring.dependency-management") version "1.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.8.10"
}

group = "com.telegrambot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation ("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.telegram:telegrambots:6.9.7.1")
    implementation("org.telegram:telegrambots-spring-boot-starter:6.9.7.1")

    implementation("org.modelmapper:modelmapper:3.2.2")

    implementation("com.github.prominence:openweathermap-api:2.4.0")
    implementation("com.github.zugaldia.noaa:ndfd:0.1")
    implementation("io.github.bonigarcia:webdrivermanager:5.9.2")

    implementation("org.apache.httpcomponents.client5:httpclient5:5.2")
    implementation("org.apache.httpcomponents.core5:httpcore5:5.2")

    implementation("com.fasterxml.jackson.core:jackson-databind")

    implementation("org.json:json:20210307")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.seleniumhq.selenium:selenium-java:4.27.0")

    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    runtimeOnly("com.h2database:h2")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation("org.slf4j:slf4j-simple:2.0.16")
    implementation("org.slf4j:slf4j-api:2.0.16")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("TgWeatherBot")
    archiveVersion.set("1.0")
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "com.telegrambot.TelegramWeatherBotApplication"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}