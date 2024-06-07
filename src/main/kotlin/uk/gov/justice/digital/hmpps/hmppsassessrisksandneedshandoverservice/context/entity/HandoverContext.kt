package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

enum class UserAccess(val value: String) {
  READ_ONLY("READ_ONLY"),
  READ_WRITE("READ_WRITE"),
  ;

  fun toAuthorities(): List<GrantedAuthority> {
    return when (this) {
      READ_ONLY -> listOf(SimpleGrantedAuthority("READ"))
      READ_WRITE -> listOf(SimpleGrantedAuthority("READ"), SimpleGrantedAuthority("WRITE"))
    }
  }
}

@RedisHash("HandoverContext")
data class HandoverContext(
  @Id @Indexed var handoverSessionId: String,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  val principal: HandoverPrincipal,
  val subject: SubjectDetails,
  val assessmentContext: AssessmentContext?,
  val sentencePlanContext: SentencePlanContext?,
)

data class HandoverPrincipal(
  val identifier: String = "",
  val displayName: String = "",
  val accessMode: UserAccess = UserAccess.READ_ONLY,
  val returnUrl: String = "",
) {
  override fun toString(): String {
    return identifier
  }
}

data class AssessmentContext(
  val oasysAssessmentPk: String,

  val assessmentVersion: Long?,
)

data class SentencePlanContext(
  val oasysPk: String,
  val assessmentVersion: String,
)

data class SubjectDetails(
  val crn: String?,
  val pnc: String?,
  val nomisId: String?,
  val givenName: String,
  val familyName: String,
  val dateOfBirth: LocalDate?,
  val gender: Int,
  val location: Location,
  val sexuallyMotivatedOffenceHistory: String?,
)

enum class Location {
  PRISON,
  COMMUNITY,
}
