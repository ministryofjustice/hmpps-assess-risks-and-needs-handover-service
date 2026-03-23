package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverAuthDetails
import java.security.Principal

data class AuthorizationCodeContext(
  val authorizationRequest: AuthorizationCodeRequest,
  val principal: AuthorizationCodePrincipal,
) {
  fun toAttributes(principalName: String): Map<String, Any> {
    val authentication = UsernamePasswordAuthenticationToken(
      principalName,
      null,
      principal.authorities.map(::SimpleGrantedAuthority),
    )
    authentication.details = principal.details

    val additionalParameters = linkedMapOf<String, Any>()
    authorizationRequest.codeChallenge?.let { additionalParameters[PkceParameterNames.CODE_CHALLENGE] = it }
    authorizationRequest.codeChallengeMethod?.let { additionalParameters[PkceParameterNames.CODE_CHALLENGE_METHOD] = it }

    val request = OAuth2AuthorizationRequest.authorizationCode()
      .authorizationUri(authorizationRequest.authorizationUri)
      .clientId(authorizationRequest.clientId)
      .redirectUri(authorizationRequest.redirectUri)
      .scopes(authorizationRequest.scopes)
      .state(authorizationRequest.state)
      .additionalParameters(additionalParameters)
      .build()

    return mapOf(
      OAuth2AuthorizationRequest::class.java.name to request,
      Principal::class.java.name to authentication,
    )
  }

  companion object {
    fun from(authorization: OAuth2Authorization): AuthorizationCodeContext? = fromAttributes(authorization.attributes)

    fun fromAttributes(attributes: Map<String, Any>): AuthorizationCodeContext? {
      val authorizationRequest = attributes[OAuth2AuthorizationRequest::class.java.name] as? OAuth2AuthorizationRequest
        ?: return null
      val authentication = attributes[Principal::class.java.name] as? Authentication
        ?: return null

      return AuthorizationCodeContext(
        authorizationRequest = AuthorizationCodeRequest(
          clientId = authorizationRequest.clientId,
          redirectUri = authorizationRequest.redirectUri,
          authorizationUri = authorizationRequest.authorizationUri,
          scopes = authorizationRequest.scopes,
          state = authorizationRequest.state,
          codeChallenge = authorizationRequest.additionalParameters[PkceParameterNames.CODE_CHALLENGE] as? String,
          codeChallengeMethod = authorizationRequest.additionalParameters[PkceParameterNames.CODE_CHALLENGE_METHOD] as? String,
        ),
        principal = AuthorizationCodePrincipal(
          authorities = authentication.authorities.mapNotNull { it.authority }.toSet(),
          details = authentication.details as? HandoverAuthDetails,
        ),
      )
    }
  }
}

data class AuthorizationCodeRequest(
  val clientId: String,
  val redirectUri: String?,
  val authorizationUri: String,
  val scopes: Set<String>,
  val state: String?,
  val codeChallenge: String?,
  val codeChallengeMethod: String?,
)

data class AuthorizationCodePrincipal(
  val authorities: Set<String>,
  val details: HandoverAuthDetails?,
)
