import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
  kotlin("jvm") version "1.7.22"
  id("org.springframework.boot") version "3.1.2"
  id("io.spring.dependency-management") version "1.1.0"
  kotlin("plugin.spring") version "1.8.20"
  kotlin("plugin.jpa") version "1.8.20"
  id("io.ktor.plugin") version "2.3.0"
  kotlin("plugin.serialization") version "1.5.0"
  id("application")
}
application {
  mainClass.set("com.npd.betting.BettingGraphqlApi")
}

group = "com.npd.betting"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
  mavenCentral()
  gradlePluginPortal()
  maven(url = "https://plugins.gradle.org/m2/")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-graphql")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-actuator")

  implementation("org.hibernate:hibernate-core:6.1.7.Final")
  implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")

  implementation("org.jetbrains.kotlin:kotlin-reflect")

  implementation("io.ktor:ktor-client-core")
  implementation("io.ktor:ktor-client-cio")
  implementation("io.ktor:ktor-client-json")
  implementation("io.ktor:ktor-client-serialization")
  implementation("io.ktor:ktor-serialization-kotlinx-json")
  implementation("io.ktor:ktor-client-content-negotiation")

  implementation("com.graphql-java:graphql-java-extended-scalars:20.0")
  implementation("com.graphql-java:graphiql-spring-boot-starter:3.0.3")
  implementation("com.graphql-java:graphql-spring-boot-starter:5.0.2")

  runtimeOnly("com.mysql:mysql-connector-j")
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "17"
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}
