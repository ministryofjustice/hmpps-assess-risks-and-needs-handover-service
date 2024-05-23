package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.ClientsProperties
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.HandoverRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.response.CreateHandoverLinkResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service.HandoverService
import java.net.URL

@RestController
@RequestMapping("/handover")
@Tag(name = "Handover", description = "APIs for handling handovers")
class HandoverController(
  private val handoverService: HandoverService,
  private val cliemtsProperties: ClientsProperties,
) {

  private val strategy = SecurityContextHolder.getContextHolderStrategy()
  private val repo = HttpSessionSecurityContextRepository()

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
  ): CreateHandoverLinkResponse {
    return handoverService.createHandover(handoverRequest)
  }

  @GetMapping("/{handoverCode}")
  @Operation(
    summary = "Use a handover link",
    description = "Consumes a handover link and exchanges it for authentication session cookie",
    responses = [
      ApiResponse(responseCode = "200", description = "Handover link exchanged successfully"),
      ApiResponse(responseCode = "400", description = "Invalid handover code"),
      ApiResponse(responseCode = "401", description = "Unauthorized"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
      ApiResponse(responseCode = "404", description = "Handover link not found"),
    ],
  )
  fun useHandoverLink(
    @Parameter(description = "Handover code") @PathVariable handoverCode: String,
    @Parameter(description = "Client ID") @RequestParam clientId: String = "sentence-plan",
    request: HttpServletRequest,
    response: HttpServletResponse,
  ) {
    val context = strategy.createEmptyContext()
    context.authentication = handoverService.consumeAndExchangeHandover(handoverCode)
    strategy.context = context
    repo.saveContext(context, request, response)

    val client = cliemtsProperties.clients.get(clientId)

    client?.let {
      response.sendRedirect(client.handoverRedirectUri)
    }
  }
}
