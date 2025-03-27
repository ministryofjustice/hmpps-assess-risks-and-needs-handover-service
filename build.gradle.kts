import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.0.0"
  kotlin("plugin.spring") version "2.1.20"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.4.2")
  implementation("org.springframework.boot:spring-boot-starter-webflux")

  // Redis dependencies
  implementation("org.springframework.data:spring-data-redis")
  implementation("org.springframework.session:spring-session-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")

  // Security/oauth2 dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.security:spring-security-config:6.4.4")
  implementation("org.springframework.security:spring-security-oauth2-authorization-server:1.3.4")

  implementation("org.bouncycastle:bcprov-jdk18on:1.80")
  implementation("org.bouncycastle:bcpkix-jdk18on:1.80")

  // Audit
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.2")

  // OpenAPI dependencies
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

  // MVC
  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
  implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
  implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.4.0")
  implementation("org.webjars.npm:govuk-frontend:5.9.0")
  implementation("org.webjars:webjars-locator-core:0.59")

  // Test dependencies
  testImplementation(kotlin("test"))
  testImplementation("net.datafaker:datafaker:2.4.2")
  testImplementation("com.ninja-squad:springmockk:4.0.2")
  testImplementation("org.wiremock:wiremock-standalone:3.12.1")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions.jvmTarget = JvmTarget.JVM_21
  }
}
