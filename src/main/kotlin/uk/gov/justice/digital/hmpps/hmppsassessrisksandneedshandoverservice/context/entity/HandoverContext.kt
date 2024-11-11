package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity

import jakarta.validation.Valid
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.validators.AppSecAllowedCharacters
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
  val criminogenicNeedsData: CriminogenicNeedsData? = null,
)

data class HandoverPrincipal(
  @field:Size(min = 1, max = 50)
  @field:Pattern(regexp = "^[a-zA-Z0-9-'\\s,./_]+$", message = "Field must contain only alphanumeric characters, hyphens, spaces, commas, full stops, forward slashes, or apostrophes and underscores" )
  val identifier: String = "",

  @field:Size(min = 1, max = 50)
  @field:AppSecAllowedCharacters
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
  @field:AppSecAllowedCharacters
  val crn: String?,
  @field:Size(min = 1, max = 15)
  @field:AppSecAllowedCharacters
  val pnc: String?,
  @field:Size(max = 50)
  @field:AppSecAllowedCharacters
  val nomisId: String?,
  @field:Size(min = 1, max = 25)
  @field:AppSecAllowedCharacters
  @field:Pattern(regexp = "^[a-zA-Z\\-']+$", message = "Given name must contain only alphabetic characters, hyphens, or apostrophes")
  val givenName: String,
  @field:Size(min = 1, max = 25)
  @field:AppSecAllowedCharacters
  @field:Pattern(regexp = "^[a-zA-Z\\-'\\s]+$", message = "Family name must contain only alphabetic characters, hyphens, spaces, or apostrophes")
  val familyName: String,
  @field:Past
  val dateOfBirth: LocalDate?,
  val gender: Int,
  val location: Location,
  @field:AppSecAllowedCharacters
  @field:Pattern(regexp = "YES|NO", message = "must be either 'YES' or 'NO'")
  val sexuallyMotivatedOffenceHistory: String?,
)

data class CriminogenicNeedsData(
  @field:Valid
  val accommodation: Accommodation? = null,
  @field:Valid
  val educationTrainingEmployability: EducationTrainingEmployability? = null,
  @field:Valid
  val finance: Finance? = null,
  @field:Valid
  val drugMisuse: DrugMisuse? = null,
  @field:Valid
  val alcoholMisuse: AlcoholMisuse? = null,
  @field:Valid
  val healthAndWellbeing: HealthAndWellbeing? = null,
  @field:Valid
  val personalRelationshipsAndCommunity: PersonalRelationshipsAndCommunity? = null,
  @field:Valid
  val thinkingBehaviourAndAttitudes: ThinkingBehaviourAndAttitudes? = null,
  @field:Valid
  val lifestyleAndAssociates: LifestyleAndAssociates? = null,
)

data class Accommodation(
  @field:AppSecAllowedCharacters
  val accLinkedToHarm: String? = null,
  @field:AppSecAllowedCharacters
  val accLinkedToReoffending: String? = null,
  @field:AppSecAllowedCharacters
  val accStrengths: String? = null,
  @field:AppSecAllowedCharacters
  val accOtherWeightedScore: String? = null,
  @field:AppSecAllowedCharacters
  val accThreshold: String? = null,
)

data class EducationTrainingEmployability(
  @field:AppSecAllowedCharacters
  val eteLinkedToHarm: String? = null,
  @field:AppSecAllowedCharacters
  val eteLinkedToReoffending: String? = null,
  @field:AppSecAllowedCharacters
  val eteStrengths: String? = null,
  @field:AppSecAllowedCharacters
  val eteOtherWeightedScore: String? = null,
  @field:AppSecAllowedCharacters
  val eteThreshold: String? = null,
)

data class Finance(
  @field:AppSecAllowedCharacters
  val financeLinkedToHarm: String? = null,
  @field:AppSecAllowedCharacters
  val financeLinkedToReoffending: String? = null,
  @field:AppSecAllowedCharacters
  val financeStrengths: String? = null,
  @field:AppSecAllowedCharacters
  val financeOtherWeightedScore: String? = null,
  @field:AppSecAllowedCharacters
  val financeThreshold: String? = null,
)

data class DrugMisuse(
  @field:AppSecAllowedCharacters
  val drugLinkedToHarm: String? = null,
  @field:AppSecAllowedCharacters
  val drugLinkedToReoffending: String? = null,
  @field:AppSecAllowedCharacters
  val drugStrengths: String? = null,
  @field:AppSecAllowedCharacters
  val drugOtherWeightedScore: String? = null,
  @field:AppSecAllowedCharacters
  val drugThreshold: String? = null,
)

data class AlcoholMisuse(
  @field:AppSecAllowedCharacters
  val alcoholLinkedToHarm: String? = null,
  @field:AppSecAllowedCharacters
  val alcoholLinkedToReoffending: String? = null,
  @field:AppSecAllowedCharacters
  val alcoholStrengths: String? = null,
  @field:AppSecAllowedCharacters
  val alcoholOtherWeightedScore: String? = null,
  @field:AppSecAllowedCharacters
  val alcoholThreshold: String? = null,
)

data class HealthAndWellbeing(
  @field:AppSecAllowedCharacters
  val emoLinkedToHarm: String? = null,
  @field:AppSecAllowedCharacters
  val emoLinkedToReoffending: String? = null,
  @field:AppSecAllowedCharacters
  val emoStrengths: String? = null,
  @field:AppSecAllowedCharacters
  val emoOtherWeightedScore: String? = null,
  @field:AppSecAllowedCharacters
  val emoThreshold: String? = null,
)

data class PersonalRelationshipsAndCommunity(
  @field:AppSecAllowedCharacters
  val relLinkedToHarm: String? = null,
  @field:AppSecAllowedCharacters
  val relLinkedToReoffending: String? = null,
  @field:AppSecAllowedCharacters
  val relStrengths: String? = null,
  @field:AppSecAllowedCharacters
  val relOtherWeightedScore: String? = null,
  @field:AppSecAllowedCharacters
  val relThreshold: String? = null,
)

data class ThinkingBehaviourAndAttitudes(
  @field:AppSecAllowedCharacters
  val thinkLinkedToHarm: String? = null,
  @field:AppSecAllowedCharacters
  val thinkLinkedToReoffending: String? = null,
  @field:AppSecAllowedCharacters
  val thinkStrengths: String? = null,
  @field:AppSecAllowedCharacters
  val thinkOtherWeightedScore: String? = null,
  @field:AppSecAllowedCharacters
  val thinkThreshold: String? = null,
)

data class LifestyleAndAssociates(
  @field:AppSecAllowedCharacters
  val lifestyleLinkedToHarm: String? = null,
  @field:AppSecAllowedCharacters
  val lifestyleLinkedToReoffending: String? = null,
  @field:AppSecAllowedCharacters
  val lifestyleStrengths: String? = null,
  @field:AppSecAllowedCharacters
  val lifestyleOtherWeightedScore: String? = null,
  @field:AppSecAllowedCharacters
  val lifestyleThreshold: String? = null,
)

enum class Location {
  PRISON,
  COMMUNITY,
}
