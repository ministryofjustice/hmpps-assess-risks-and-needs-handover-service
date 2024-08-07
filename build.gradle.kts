import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.2"
  kotlin("plugin.spring") version "2.0.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")

  // Redis dependencies
  implementation("org.springframework.data:spring-data-redis")
  implementation("org.springframework.session:spring-session-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")

  // Security/oauth2 dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.security:spring-security-config")
  implementation("org.springframework.security:spring-security-oauth2-authorization-server")

  implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
  implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")

  // OpenAPI dependencies
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

  // Test dependencies
  testImplementation(kotlin("test"))
  testImplementation("net.datafaker:datafaker:2.3.1")
  testImplementation("com.ninja-squad:springmockk:4.0.2")
  testImplementation("org.wiremock:wiremock-standalone:3.9.1")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions.jvmTarget = JvmTarget.JVM_21
  }
}
