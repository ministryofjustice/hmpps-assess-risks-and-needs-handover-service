package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.extensions.isSubdomainOf
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.CreateHandoverLinkRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.response.CreateHandoverLinkResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service.HandoverService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service.UseHandoverLinkResult
import java.net.URI
import java.util.*

@RestController
@Validated
@RequestMapping("\${app.self.endpoints.handover}")
@Tag(name = "Handover", description = "Endpoints for creating and consuming a handover session")
class HandoverController(
  private val handoverService: HandoverService,
  private val appConfiguration: AppConfiguration,
) {
  @PreAuthorize("@jwt.isIssuedByHmppsAuth() and @jwt.isClientCredentialsGrant()")
  @PostMapping
  @Operation(
    summary = "Create a new handover link",
    description = "Creates a new handover link using the provided handover request. " +
      "**Authorization for this endpoint requires a client credentials JWT provided by HMPPS Auth.**",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Request example",
      content = arrayOf(
        Content(
          mediaType = "application/json",
          examples = arrayOf(
            ExampleObject(
              name = "Handover Request",
              value = HANDOVER_REQUEST_EXAMPLE,
            ),
          ),
        ),
      ),
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Handover link created successfully",
        content = [Content(schema = Schema(implementation = CreateHandoverLinkResponse::class))],
      ),
      ApiResponse(responseCode = "400", description = "Invalid request"),
      ApiResponse(responseCode = "401", description = "Unauthorized"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
    ],
  )
  fun createHandoverLink(
    @RequestBody @Valid handoverRequest: CreateHandoverLinkRequest,
  ): ResponseEntity<CreateHandoverLinkResponse> = ResponseEntity.ok(handoverService.createHandover(handoverRequest))

  @GetMapping("/{handoverCode}")
  @Operation(
    summary = "Use a handover link",
    description = "Consumes a handover link and exchanges it for authentication session cookie",
    responses = [
      ApiResponse(responseCode = "200", description = "Handover link exchanged successfully"),
      ApiResponse(responseCode = "404", description = "Client not found"),
      ApiResponse(responseCode = "404", description = "Handover link expired or not found"),
      ApiResponse(responseCode = "409", description = "Handover link has already been used"),
    ],
  )
  fun useHandoverLink(
    @Parameter(description = "Handover code") @PathVariable handoverCode: UUID,
    @Parameter(description = "Client ID") @RequestParam clientId: String,
    @Parameter(description = "Redirect URI") @RequestParam(required = false) redirectUri: String?,
    request: HttpServletRequest,
    response: HttpServletResponse,
  ): ResponseEntity<Any> {
    val strategy = SecurityContextHolder.getContextHolderStrategy()
    val repo = HttpSessionSecurityContextRepository()

    val accessDenied = ResponseEntity
      .status(HttpStatus.FOUND)
      .header("Location", "/access-denied")
      .build<Any?>()

    val client = appConfiguration.clients[clientId]
      ?: return accessDenied.also { log.info("Client not found") }

    val finalRedirectUri = redirectUri
      ?.takeIf { it.isNotBlank() }
      ?.let { uriString ->
        val uri = URI(uriString)
        val clientRedirectUri = URI(client.handoverRedirectUri)

        if (uri != clientRedirectUri && !uri.isSubdomainOf(clientRedirectUri)) {
          return accessDenied.also {
            log.info(
              "Invalid redirect URI: {} does not match or is not a subdomain of configured URI: {}",
              uriString,
              client.handoverRedirectUri,
            )
          }
        }

        uri.toString()
      } ?: client.handoverRedirectUri

    return when (val result = handoverService.consumeAndExchangeHandover(handoverCode)) {
      is UseHandoverLinkResult.Success -> {
        strategy.context = strategy.createEmptyContext()
        strategy.context.authentication = result.authenticationToken
        repo.saveContext(strategy.context, request, response)

        ResponseEntity
          .status(HttpStatus.FOUND)
          .header("Location", finalRedirectUri)
          .build()
      }
      UseHandoverLinkResult.HandoverLinkNotFound -> accessDenied.also { log.info("Handover link expired or not found") }
      UseHandoverLinkResult.HandoverLinkAlreadyUsed -> accessDenied.also { log.info("Handover link has already been used") }
    }
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    const val HANDOVER_REQUEST_EXAMPLE = """{
      "user": {
        "identifier": "RBACKENT",
        "displayName": "KENT Assessor",
        "accessMode": "READ_WRITE",
        "returnUrl": "http://192.168.56.21:8080/ords/f?p=EORSAN010:SAN010_LANDING:11281584380154::NO:APP::&cs=3h8xw8SUx3QRT6kfPRzfTI31XMtHvLSh90b9Yw4EPgxDIUaIjBgoYndHHlCjtWwUanAQjejASWA1a7E6M6LIqLw"
      },
      "subjectDetails": {
        "crn": "D25987M",
        "pnc": "22/2083Y",
        "nomisId": null,
        "givenName": "Paul",
        "familyName": "PINK",
        "dateOfBirth": "1954-10-16",
        "gender": "1",
        "location": "COMMUNITY",
        "sexuallyMotivatedOffenceHistory": "NO"
      },
      "oasysAssessmentPk": 1631219,
      "assessmentVersion": null,
      "planVersion": null
    }"""
  }
}
