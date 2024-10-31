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

data class CriminogenicNeedsData(
  val accommodation: Accommodation? = null,
  val educationTrainingEmployability: EducationTrainingEmployability? = null,
  val finance: Finance? = null,
  val drugMisuse: DrugMisuse? = null,
  val alcoholMisuse: AlcoholMisuse? = null,
  val healthAndWellbeing: HealthAndWellbeing? = null,
  val personalRelationshipsAndCommunity: PersonalRelationshipsAndCommunity? = null,
  val thinkingBehaviourAndAttitudes: ThinkingBehaviourAndAttitudes? = null,
  val lifestyleAndAssociates: LifestyleAndAssociates? = null,
)

data class Accommodation(
  val accLinkedToHarm: String? = null,
  val accLinkedToReoffending: String? = null,
  val accStrengths: String? = null,
  val accOtherWeightedScore: String? = null,
  val accThreshold: String? = null,
)

data class EducationTrainingEmployability(
  val eteLinkedToHarm: String? = null,
  val eteLinkedToReoffending: String? = null,
  val eteStrengths: String? = null,
  val eteOtherWeightedScore: String? = null,
  val eteThreshold: String? = null,
)

data class Finance(
  val financeLinkedToHarm: String? = null,
  val financeLinkedToReoffending: String? = null,
  val financeStrengths: String? = null,
  val financeOtherWeightedScore: String? = null,
  val financeThreshold: String? = null,
)

data class DrugMisuse(
  val drugLinkedToHarm: String? = null,
  val drugLinkedToReoffending: String? = null,
  val drugStrengths: String? = null,
  val drugOtherWeightedScore: String? = null,
  val drugThreshold: String? = null,
)

data class AlcoholMisuse(
  val alcoholLinkedToHarm: String? = null,
  val alcoholLinkedToReoffending: String? = null,
  val alcoholStrengths: String? = null,
  val alcoholOtherWeightedScore: String? = null,
  val alcoholThreshold: String? = null,
)

data class HealthAndWellbeing(
  val emoLinkedToHarm: String? = null,
  val emoLinkedToReoffending: String? = null,
  val emoStrengths: String? = null,
  val emoOtherWeightedScore: String? = null,
  val emoThreshold: String? = null,
)

data class PersonalRelationshipsAndCommunity(
  val relLinkedToHarm: String? = null,
  val relLinkedToReoffending: String? = null,
  val relStrengths: String? = null,
  val relOtherWeightedScore: String? = null,
  val relThreshold: String? = null,
)

data class ThinkingBehaviourAndAttitudes(
  val thinkLinkedToHarm: String? = null,
  val thinkLinkedToReoffending: String? = null,
  val thinkStrengths: String? = null,
  val thinkOtherWeightedScore: String? = null,
  val thinkThreshold: String? = null,
)

data class LifestyleAndAssociates(
  val lifestyleLinkedToHarm: String? = null,
  val lifestyleLinkedToReoffending: String? = null,
  val lifestyleStrengths: String? = null,
  val lifestyleOtherWeightedScore: String? = null,
  val lifestyleThreshold: String? = null,
)

enum class Location {
  PRISON,
  COMMUNITY,
}
