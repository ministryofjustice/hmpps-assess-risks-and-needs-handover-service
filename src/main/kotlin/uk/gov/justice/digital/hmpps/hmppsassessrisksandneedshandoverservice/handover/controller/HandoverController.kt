package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.CreateHandoverLinkRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.response.CreateHandoverLinkResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service.HandoverService

@RestController
@Validated
@RequestMapping("\${app.self.endpoints.handover}")
@Tag(name = "Handover", description = "Endpoints for creating and consuming a handover session")
class HandoverController(
  private val handoverService: HandoverService,
) {
  @PreAuthorize("@jwt.isIssuedByHmppsAuth() and @jwt.isClientCredentialsGrant()")
  @PostMapping
  @Operation(
    summary = "Create a new handover link",
    description = "Creates a new handover link using the provided handover request. " +
      "**Authorization for this endpoint requires a client credentials JWT provided by HMPPS Auth.**",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Request example",
      content = arrayOf(
        Content(
          mediaType = "application/json",
          examples = arrayOf(
            ExampleObject(
              name = "Handover Request",
              value = HANDOVER_REQUEST_EXAMPLE,
            ),
          ),
        ),
      ),
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Handover link created successfully",
        content = [Content(schema = Schema(implementation = CreateHandoverLinkResponse::class))],
      ),
      ApiResponse(responseCode = "400", description = "Invalid request"),
      ApiResponse(responseCode = "401", description = "Unauthorized"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
    ],
  )
  suspend fun createHandoverLink(
    @RequestBody @Valid handoverRequest: CreateHandoverLinkRequest,
  ): ResponseEntity<CreateHandoverLinkResponse> = ResponseEntity.ok(handoverService.createHandover(handoverRequest))

  private companion object {
    const val HANDOVER_REQUEST_EXAMPLE = """{
      "user": {
        "identifier": "RBACKENT",
        "displayName": "KENT Assessor",
        "accessMode": "READ_WRITE",
        "returnUrl": "http://192.168.56.21:8080/ords/f?p=EORSAN010:SAN010_LANDING:11281584380154::NO:APP::&cs=3h8xw8SUx3QRT6kfPRzfTI31XMtHvLSh90b9Yw4EPgxDIUaIjBgoYndHHlCjtWwUanAQjejASWA1a7E6M6LIqLw"
      },
      "subjectDetails": {
        "crn": "D25987M",
        "pnc": "22/2083Y",
        "nomisId": null,
        "givenName": "Paul",
        "familyName": "PINK",
        "dateOfBirth": "1954-10-16",
        "gender": "1",
        "location": "COMMUNITY",
        "sexuallyMotivatedOffenceHistory": "NO"
      },
      "oasysAssessmentPk": 1631219,
      "assessmentVersion": null,
      "planVersion": null
    }"""
  }
}
