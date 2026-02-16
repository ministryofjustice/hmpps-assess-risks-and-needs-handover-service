package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.AssessmentContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SentencePlanContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service.GetHandoverContextResult
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service.HandoverContextService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.coordinator.service.CoordinatorService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.events.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.events.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverAuthDetails
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverToken
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.TokenStatus
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.repository.HandoverTokenRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.CreateHandoverLinkRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.response.CreateHandoverLinkResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.service.TelemetryService
import java.util.*

enum class TokenValidationResult {
  VALID,
  NOT_FOUND,
  ALREADY_USED,
}

@Service
class HandoverService(
  val handoverTokenRepository: HandoverTokenRepository,
  val handoverContextService: HandoverContextService,
  val coordinatorService: CoordinatorService,
  val appConfiguration: AppConfiguration,
  val telemetryService: TelemetryService,
  val auditService: AuditService,
) {

  fun createHandover(
    handoverRequest: CreateHandoverLinkRequest,
    handoverSessionId: UUID = UUID.randomUUID(),
  ): CreateHandoverLinkResponse {
    val associations = coordinatorService.getAssociations(handoverRequest.oasysAssessmentPk)
    val handoverToken = HandoverToken(
      handoverSessionId = handoverSessionId,
      principal = handoverRequest.user,
    )
    val handoverContext = HandoverContext(
      handoverSessionId = handoverSessionId,
      principal = handoverRequest.user,
      subject = handoverRequest.subjectDetails,
      assessmentContext = AssessmentContext(
        oasysAssessmentPk = handoverRequest.oasysAssessmentPk,
        assessmentId = associations.sanAssessmentId,
        assessmentVersion = handoverRequest.assessmentVersion,
      ),
      sentencePlanContext = SentencePlanContext(
        oasysAssessmentPk = handoverRequest.oasysAssessmentPk,
        planId = associations.sentencePlanId,
        planVersion = handoverRequest.sentencePlanVersion,
      ),
      criminogenicNeedsData = handoverRequest.criminogenicNeedsData,
    )

    handoverContextService.saveContext(handoverContext)
    handoverTokenRepository.save(handoverToken)
    val handoverLink = generateHandoverLink(handoverToken.code)

    telemetryService.track(TelemetryEvent.ONE_TIME_LINK_CREATED, handoverContext)
    publishAuditEvent(AuditEvent.ONE_TIME_LINK_CREATED, handoverContext)

    return CreateHandoverLinkResponse(
      handoverLink = handoverLink,
      handoverSessionId = handoverSessionId,
      link = handoverLink,
    )
  }

  fun consumeAndExchangeHandover(handoverCode: UUID): UseHandoverLinkResult = when (validateToken(handoverCode)) {
    TokenValidationResult.NOT_FOUND -> UseHandoverLinkResult.HandoverLinkNotFound
    TokenValidationResult.ALREADY_USED -> UseHandoverLinkResult.HandoverLinkAlreadyUsed
    TokenValidationResult.VALID -> {
      val handoverToken = consumeToken(handoverCode)
      val handoverSessionId = handoverToken.handoverSessionId
      val contextResult = handoverContextService.getContext(handoverSessionId)

      if (contextResult is GetHandoverContextResult.Success) {
        telemetryService.track(TelemetryEvent.ONE_TIME_LINK_USED, contextResult.handoverContext)
        publishAuditEvent(AuditEvent.ONE_TIME_LINK_USED, contextResult.handoverContext)

        val authToken = UsernamePasswordAuthenticationToken(
          contextResult.handoverContext.principal.identifier,
          null,
          contextResult.handoverContext.principal.accessMode.toAuthorities(),
        )
        authToken.details = HandoverAuthDetails(
          handoverSessionId = handoverSessionId,
          principal = contextResult.handoverContext.principal,
        )
        UseHandoverLinkResult.Success(authToken)
      } else {
        UseHandoverLinkResult.HandoverLinkNotFound
      }
    }
  }

  private fun generateHandoverLink(handoverCode: UUID): String = "${appConfiguration.self.externalUrl}${appConfiguration.self.endpoints.handover}/$handoverCode"

  private fun validateToken(code: UUID): TokenValidationResult {
    val token = handoverTokenRepository.findById(code).orElse(null)
      ?: return TokenValidationResult.NOT_FOUND

    if (token.tokenStatus == TokenStatus.USED) {
      return TokenValidationResult.ALREADY_USED
    }

    return TokenValidationResult.VALID
  }

  private fun consumeToken(code: UUID): HandoverToken {
    val token = handoverTokenRepository.findById(code).orElse(null)
      ?: throw NoSuchElementException()

    token.tokenStatus = TokenStatus.USED
    return handoverTokenRepository.save(token)
  }

  private fun publishAuditEvent(
    event: AuditEvent,
    context: HandoverContext,
  ) {
    auditService.publish(
      event = event,
      who = context.principal.identifier,
      subjectId = context.subject.crn,
    )
  }
}

sealed class UseHandoverLinkResult {
  data class Success(val authenticationToken: UsernamePasswordAuthenticationToken) : UseHandoverLinkResult()
  data object HandoverLinkNotFound : UseHandoverLinkResult()
  data object HandoverLinkAlreadyUsed : UseHandoverLinkResult()
}
