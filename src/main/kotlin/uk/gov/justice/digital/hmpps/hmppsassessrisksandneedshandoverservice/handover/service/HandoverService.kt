package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service

import org.springframework.beans.factory.annotation.Value
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
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverToken
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.TokenStatus
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.repository.HandoverTokenRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.CreateHandoverLinkRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.response.CreateHandoverLinkResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.service.TelemetryService
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService
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
  val auditService: HmppsAuditService,
) {
  suspend fun createHandover(
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

  suspend fun consumeAndExchangeHandover(handoverCode: UUID): UseHandoverLinkResult = when (validateToken(handoverCode)) {
    TokenValidationResult.NOT_FOUND -> UseHandoverLinkResult.HandoverLinkNotFound
    TokenValidationResult.ALREADY_USED -> UseHandoverLinkResult.HandoverLinkAlreadyUsed
    TokenValidationResult.VALID -> {
      val handoverSessionId = consumeToken(handoverCode).handoverSessionId
      handoverContextService.getContext(handoverSessionId).let {
        if (it is GetHandoverContextResult.Success) {
          telemetryService.track(TelemetryEvent.ONE_TIME_LINK_USED, it.handoverContext)
          publishAuditEvent(AuditEvent.ONE_TIME_LINK_USED, it.handoverContext)
        }
      }
      UseHandoverLinkResult.Success(
        UsernamePasswordAuthenticationToken(
          handoverSessionId.toString(),
          null,
          null,
        ),
      )
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

  private suspend fun publishAuditEvent(
    event: AuditEvent,
    context: HandoverContext,
    @Value("\${spring.application.name}")
    service: String = "",
  ) = auditService.publishEvent(
    what = event.name,
    subjectId = context.subject.crn,
    subjectType = "CRN",
    who = context.principal.identifier,
    service = service,
  )
}

sealed class UseHandoverLinkResult {
  data class Success(val authenticationToken: UsernamePasswordAuthenticationToken) : UseHandoverLinkResult()
  data object HandoverLinkNotFound : UseHandoverLinkResult()
  data object HandoverLinkAlreadyUsed : UseHandoverLinkResult()
}
