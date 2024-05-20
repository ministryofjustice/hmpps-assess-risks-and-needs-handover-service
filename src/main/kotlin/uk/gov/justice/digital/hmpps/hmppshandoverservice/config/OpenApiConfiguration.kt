package uk.gov.justice.digital.hmpps.hmppshandoverservice.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Configuration
class OpenApiConfiguration(
  buildProperties: BuildProperties,
) {
  private val version: String = buildProperties.version

  init {
    val schema = Schema<LocalDateTime>()
    schema.example(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))
    SpringDocUtils.getConfig().replaceWithSchema(LocalDateTime::class.java, schema)
  }

  @Bean
  fun customOpenAPI(buildProperties: BuildProperties, appConfiguration: AppConfiguration): OpenAPI? = OpenAPI()
    .servers(
      listOf(
        Server().url(appConfiguration.baseUrl),
      ),
    )
    .openapi("3.0.1")
    .info(
      Info()
        .title("HMPPS Handover Service")
        .version(version)
        .description("Authentication and management of context data for applications in the ARNS space")
        .contact(
          Contact()
            .name("HMPPS Digital Studio")
            .email("feedback@digital.justice.gov.uk")
            .url("https://github.com/ministryofjustice/hmpps-handover-service"),
        ),
    )
    .components(
      Components().addSecuritySchemes(
        "bearer-jwt",
        SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
          .`in`(SecurityScheme.In.HEADER)
          .name("Authorization"),
      ),
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt"))
}
