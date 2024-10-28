package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity

import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.validators.OasysReturnUrl
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
  @Id @Indexed var handoverSessionId: UUID,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  val principal: HandoverPrincipal,
  val subject: SubjectDetails,
  val assessmentContext: AssessmentContext?,
  val sentencePlanContext: SentencePlanContext?,
)

data class HandoverPrincipal(
  @field:Size(min = 1, max = 50)
  val identifier: String = "",

  @field:Size(min = 1, max = 50)
  @field:Pattern(regexp = "^[a-zA-Z\\-'\\s]+$", message = "Display name must contain only alphabetic characters, hyphens, spaces, or apostrophes")
  val displayName: String = "",
  val accessMode: UserAccess = UserAccess.READ_ONLY,

  @field:OasysReturnUrl
  val returnUrl: String = "",
) {
  override fun toString(): String {
    return identifier
  }
}

data class AssessmentContext(
  @field:Size(min = 1, max = 15)
  val oasysAssessmentPk: String,
  val assessmentId: UUID?,
  val assessmentVersion: Long?,
)

data class SentencePlanContext(
  @field:Size(min = 1, max = 15)
  val oasysAssessmentPk: String,
  val planId: UUID?,
  val planVersion: Long?,
)

data class SubjectDetails(
  @field:Size(min = 1, max = 15)
  val crn: String?,
  @field:Size(min = 1, max = 15)
  val pnc: String?,
  @field:Size(max = 50)
  val nomisId: String?,
  @field:Size(min = 1, max = 25)
  @field:Pattern(regexp = "^[a-zA-Z\\-']+$", message = "Given name must contain only alphabetic characters, hyphens, or apostrophes")
  val givenName: String,
  @field:Size(min = 1, max = 25)
  @field:Pattern(regexp = "^[a-zA-Z\\-'\\s]+$", message = "Family name must contain only alphabetic characters, hyphens, spaces, or apostrophes")
  val familyName: String,
  @field:Past
  val dateOfBirth: LocalDate?,
  val gender: Int,
  val location: Location,
  @field:Pattern(regexp = "YES|NO", message = "must be either 'YES' or 'NO'")
  val sexuallyMotivatedOffenceHistory: String?,
)

enum class Location {
  PRISON,
  COMMUNITY,
}
