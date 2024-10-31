package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.AssessmentContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SentencePlanContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service.HandoverContextService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.coordinator.service.CoordinatorService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverToken
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.TokenStatus
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.repository.HandoverTokenRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.CreateHandoverLinkRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.response.CreateHandoverLinkResponse
import java.util.*
import kotlin.NoSuchElementException

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
        assessmentVersion = handoverRequest.sanAssessmentVersion,
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

    return CreateHandoverLinkResponse(
      handoverLink = handoverLink,
      handoverSessionId = handoverSessionId,
      link = handoverLink,
    )
  }

  fun consumeAndExchangeHandover(handoverCode: UUID): UseHandoverLinkResult {
    return when (validateToken(handoverCode)) {
      TokenValidationResult.NOT_FOUND -> UseHandoverLinkResult.HandoverLinkNotFound
      TokenValidationResult.ALREADY_USED -> UseHandoverLinkResult.HandoverLinkAlreadyUsed
      TokenValidationResult.VALID -> {
        val handoverSessionId = consumeToken(handoverCode).handoverSessionId
        UseHandoverLinkResult.Success(
          UsernamePasswordAuthenticationToken(
            handoverSessionId.toString(),
            null,
            null,
          ),
        )
      }
    }
  }

  private fun generateHandoverLink(handoverCode: UUID): String {
    return "${appConfiguration.self.externalUrl}${appConfiguration.self.endpoints.handover}/$handoverCode"
  }

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

  companion object {
    private val log = LoggerFactory.getLogger(HandoverService::class.java)
  }
}

sealed class UseHandoverLinkResult {
  data class Success(val authenticationToken: UsernamePasswordAuthenticationToken) : UseHandoverLinkResult()
  data object HandoverLinkNotFound : UseHandoverLinkResult()
  data object HandoverLinkAlreadyUsed : UseHandoverLinkResult()
}
