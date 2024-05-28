package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.HandoverRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.response.CreateHandoverLinkResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service.HandoverService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service.UseHandoverLinkResult

@RestController
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
    @RequestBody handoverRequest: HandoverRequest,
  ): ResponseEntity<CreateHandoverLinkResponse> {
    return ResponseEntity.ok(handoverService.createHandover(handoverRequest))
  }

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
    @Parameter(description = "Handover code") @PathVariable handoverCode: String,
    @Parameter(description = "Client ID") @RequestParam clientId: String = "sentence-plan",
    request: HttpServletRequest,
    response: HttpServletResponse,
  ): ResponseEntity<Any> {
    val strategy = SecurityContextHolder.getContextHolderStrategy()
    val repo = HttpSessionSecurityContextRepository()
    val client = appConfiguration.clients[clientId]
      ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Client not found")

    return when (val result = handoverService.consumeAndExchangeHandover(handoverCode)) {
      is UseHandoverLinkResult.Success -> {
        strategy.context = strategy.createEmptyContext()
        strategy.context.authentication = result.authenticationToken
        repo.saveContext(strategy.context, request, response)

        ResponseEntity.status(HttpStatus.FOUND).header("Location", client.handoverRedirectUri).build()
      }
      UseHandoverLinkResult.HandoverLinkNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Handover link expired or not found")
      UseHandoverLinkResult.HandoverLinkAlreadyUsed -> ResponseEntity.status(HttpStatus.CONFLICT).body("Handover link has already been used")
    }
  }
}
