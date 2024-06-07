package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity.JpaAuthorization

interface JpaAuthorizationRepository : CrudRepository<JpaAuthorization, String> {

  fun findByAuthorizationCodeValue(token: String): JpaAuthorization?

  fun findByAccessTokenValue(token: String): JpaAuthorization?

  fun findByRefreshTokenValue(token: String): JpaAuthorization?

  fun findByOidcIdTokenValue(token: String): JpaAuthorization?

  fun findByUserCodeValue(token: String): JpaAuthorization?

  fun findByDeviceCodeValue(token: String): JpaAuthorization?

  fun findByState(token: String): JpaAuthorization?
}
