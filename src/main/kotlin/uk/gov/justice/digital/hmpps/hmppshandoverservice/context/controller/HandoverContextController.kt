package uk.gov.justice.digital.hmpps.hmppshandoverservice.context.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppshandoverservice.context.service.HandoverContextService
import uk.gov.justice.digital.hmpps.hmppshandoverservice.handover.request.HandoverRequest

@RestController
@RequestMapping("/context")
class HandoverContextController(
  val handoverContextService: HandoverContextService,
) {

  @PreAuthorize("@jwt.isIssuedByHmppsAuth() and @jwt.isClientCredentialsGrant()")
  @PostMapping("/{handoverSessionId}")
  fun updateContext(
    @PathVariable handoverSessionId: String,
    @RequestBody handoverRequest: HandoverRequest,
  ): HandoverContext? {
    return handoverContextService.updateContext(handoverSessionId, handoverRequest)
  }

  @PreAuthorize("@jwt.isIssuedByHmppsAuth() and @jwt.isClientCredentialsGrant()")
  @GetMapping("/{handoverSessionId}")
  fun getContextByHandoverSessionId(
    @PathVariable handoverSessionId: String,
  ): HandoverContext? {
    return handoverContextService.getContext(handoverSessionId)
  }

  @PreAuthorize("@jwt.isIssuedByHmppsHandover()")
  @GetMapping()
  fun getContextByAuthentication(): HandoverContext? {
    val handoverSessionId: String = SecurityContextHolder.getContext().authentication.name
    return handoverContextService.getContext(handoverSessionId)
  }
}
