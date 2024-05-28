package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.slf4j.LoggerFactory
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(
  private val appConfiguration: AppConfiguration,
) {

  init {
    log.info("OpenAPI Swagger Docs available at: ${appConfiguration.self.externalUrl}/swagger-ui.html")
  }

  @Bean
  fun customOpenAPI(buildProperties: BuildProperties, appConfiguration: AppConfiguration): OpenAPI =
    OpenAPI()
      .info(
        Info()
          .title("HMPPS ARNS Handover Service")
          .version(buildProperties.version)
          .description("Authentication and management of context data for applications in the ARNS space")
          .contact(
            Contact()
              .name("HMPPS ARNS Handover GitHub Project")
              .url("https://github.com/ministryofjustice/hmpps-asses-risk-and-needs-handover-service"),
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

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
