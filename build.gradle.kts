import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.Delete
import org.gradle.api.file.FileTree

tasks.register("stage", Delete::class) {
  dependsOn("build")

  doLast {
    val dir = fileTree("build") {
      exclude("libs")
    }
    delete(dir)
    val libsDir = fileTree("build/libs") {
      exclude("*.jar")
    }
    delete(libsDir)
  }
}

plugins {
  kotlin("jvm") version "1.7.22"
  id("org.springframework.boot") version "3.0.6"
  id("io.spring.dependency-management") version "1.1.0"
  kotlin("plugin.spring") version "1.8.20"
  kotlin("plugin.jpa") version "1.8.20"
  id("io.ktor.plugin") version "2.3.0"
  kotlin("plugin.serialization") version "1.5.0"
  id("com.github.johnrengelman.shadow") version "7.1.2"
  id("application")
}
application {
  mainClass.set("com.npd.betting.BettingGraphqlApi")
}

group = "com.npd.betting"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-graphql")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-actuator")

//  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
//  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.0-RC") // Replace with the latest version

  implementation("io.ktor:ktor-client-core")
  implementation("io.ktor:ktor-client-cio")
  implementation("io.ktor:ktor-client-json")
  implementation("io.ktor:ktor-client-serialization")
  //implementation("io.ktor:ktor-client-websockets")
  implementation("io.ktor:ktor-serialization-kotlinx-json")
  implementation("io.ktor:ktor-client-content-negotiation")
  //implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

  implementation("com.graphql-java:graphql-java-extended-scalars:20.0")
  implementation("com.graphql-java:graphiql-spring-boot-starter:3.0.3")
  implementation("com.graphql-java:graphql-spring-boot-starter:5.0.2")

  runtimeOnly("com.mysql:mysql-connector-j")

//  testImplementation("org.springframework.boot:spring-boot-starter-test")
//  testImplementation("org.springframework.graphql:spring-graphql-test")
//  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
//  testImplementation("io.mockk:mockk:1.12.0")
//  testImplementation("io.ktor:ktor-client-mock")
}

tasks {
  withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("ultrabet")
    archiveClassifier.set("")
    archiveVersion.set("")

    manifest {
      attributes["Main-Class"] = "com.npd.betting.BettingGraphqlApi"
    }

    // Set the main class name for the Shadow JAR plugin
    project.setProperty("mainClassName", "com.npd.betting.BettingGraphqlApi")
  }
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
