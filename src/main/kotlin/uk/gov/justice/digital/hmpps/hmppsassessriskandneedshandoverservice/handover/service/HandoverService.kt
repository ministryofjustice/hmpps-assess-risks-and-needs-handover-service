package uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.handover.service

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.context.service.HandoverContextService
import uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.handover.entity.HandoverToken
import uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.handover.entity.TokenStatus
import uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.handover.repository.HandoverTokenRepository
import uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.handover.request.HandoverRequest
import uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.handover.response.CreateHandoverLinkResponse
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
  val appConfiguration: AppConfiguration,
) {
  fun createHandover(
    handoverRequest: HandoverRequest,
    handoverSessionId: String = UUID.randomUUID().toString(),
  ): CreateHandoverLinkResponse {
    val handoverToken = HandoverToken(
      handoverSessionId = handoverSessionId,
      principal = handoverRequest.principal,
    )

    handoverContextService.saveContext(handoverSessionId, handoverRequest)
    handoverTokenRepository.save(handoverToken)

    return CreateHandoverLinkResponse(generateHandoverLink(handoverToken.code), handoverSessionId)
  }

  fun generateHandoverLink(handoverCode: String): String {
    return appConfiguration.baseUrl + appConfiguration.endpoints.handover + '/' + handoverCode
  }

  fun consumeAndExchangeHandover(handoverCode: String): UsernamePasswordAuthenticationToken {
    when (validateToken(handoverCode)) {
      TokenValidationResult.NOT_FOUND -> throw NoSuchElementException()
      TokenValidationResult.ALREADY_USED -> throw NoSuchElementException()
      TokenValidationResult.VALID -> {
        val handoverSessionId = consumeToken(handoverCode).handoverSessionId

        return UsernamePasswordAuthenticationToken(
          handoverSessionId,
          null,
          null,
        )
      }
    }
  }

  private fun validateToken(code: String): TokenValidationResult {
    val token = handoverTokenRepository.findById(code).orElse(null)
      ?: return TokenValidationResult.NOT_FOUND

    if (token.tokenStatus == TokenStatus.USED) {
      return TokenValidationResult.ALREADY_USED
    }

    return TokenValidationResult.VALID
  }

  private fun consumeToken(code: String): HandoverToken {
    val token = handoverTokenRepository.findById(code).orElse(null)
      ?: throw NoSuchElementException()

    token.tokenStatus = TokenStatus.USED
    return handoverTokenRepository.save(token)
  }

  companion object {
    private val log = LoggerFactory.getLogger(HandoverService::class.java)
  }
}
