package uk.gov.justice.digital.hmpps.hmppshandoverservice.handover.controller

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
import uk.gov.justice.digital.hmpps.hmppshandoverservice.handover.request.HandoverRequest
import uk.gov.justice.digital.hmpps.hmppshandoverservice.handover.response.CreateHandoverLinkResponse
import uk.gov.justice.digital.hmpps.hmppshandoverservice.handover.service.HandoverService
import java.net.URL

@RestController
@RequestMapping("/handover")
class HandoverController(
  private val handoverService: HandoverService,
  private val registeredClientRepository: RegisteredClientRepository,
) {

  private val strategy = SecurityContextHolder.getContextHolderStrategy()
  private val repo = HttpSessionSecurityContextRepository()

  @PreAuthorize("@jwt.isIssuedByHmppsAuth() and @jwt.isClientCredentialsGrant()")
  @PostMapping
  fun createHandoverLink(
    @RequestBody handoverRequest: HandoverRequest,
  ): CreateHandoverLinkResponse {
    return handoverService.createHandover(handoverRequest)
  }

  // TODO: Probably shouldn't have a default clientId for this
  @GetMapping("/{handoverCode}")
  fun useHandoverLink(
    @PathVariable
    handoverCode: String,
    @RequestParam
    clientId: String = "sentence-plan",
    request: HttpServletRequest,
    response: HttpServletResponse,
  ) {
    val context = strategy.createEmptyContext()
    context.authentication = handoverService.consumeAndExchangeHandover(handoverCode)
    strategy.context = context
    repo.saveContext(context, request, response)

    val client = registeredClientRepository.findByClientId(clientId)

    // TODO: Bit of a hacky to redirect the user back to the client, better way?
    client?.let {
      response.sendRedirect(stripPath(client.redirectUris.iterator().next()))
    }
  }

  private fun stripPath(urlString: String): String {
    val url = URL(urlString)
    val scheme = url.protocol
    val host = url.host
    val port = url.port

    return if (port == -1) {
      "$scheme://$host"
    } else {
      "$scheme://$host:$port"
    }
  }
}
