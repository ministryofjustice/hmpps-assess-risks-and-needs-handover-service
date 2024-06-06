package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity.Authorization

interface AuthorizationRepository : CrudRepository<Authorization, String> {

  fun findByAuthorizationCodeValue(token: String): Authorization?

  fun findByAccessTokenValue(token: String): Authorization?

  fun findByRefreshTokenValue(token: String): Authorization?

  fun findByOidcIdTokenValue(token: String): Authorization?

  fun findByUserCodeValue(token: String): Authorization?

  fun findByDeviceCodeValue(token: String): Authorization?

  fun findByState(token: String): Authorization?
}
