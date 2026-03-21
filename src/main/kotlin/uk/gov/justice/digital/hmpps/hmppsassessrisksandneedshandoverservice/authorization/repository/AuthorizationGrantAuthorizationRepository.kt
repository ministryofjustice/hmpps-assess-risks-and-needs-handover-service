package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity.AuthorizationCodeGrantAuthorization

interface AuthorizationGrantAuthorizationRepository : CrudRepository<AuthorizationCodeGrantAuthorization, String> {
  fun findByState(state: String): AuthorizationCodeGrantAuthorization?

  fun findByAuthorizationCode_TokenValue(tokenValue: String): AuthorizationCodeGrantAuthorization?

  fun findByStateOrAuthorizationCode_TokenValue(
    state: String,
    tokenValue: String,
  ): AuthorizationCodeGrantAuthorization?

  fun findByAccessToken_TokenValue(tokenValue: String): AuthorizationCodeGrantAuthorization?
}
