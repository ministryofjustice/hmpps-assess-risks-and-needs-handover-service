package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handlers.exceptions

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class HmppsAssessRisksAndNeedsHandoverServiceExceptionHandler {
  @ExceptionHandler(value = [ValidationException::class, MethodArgumentNotValidException::class])
  fun handleValidationExceptions(ex: Exception): ResponseEntity<ErrorResponse> {
    val userMessage: String
    val developerMessage: String

    when (ex) {
      is MethodArgumentNotValidException -> {
        val errors = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        userMessage = "Validation failure: $errors"
        developerMessage = errors
      }

      is ValidationException -> {
        userMessage = "Validation failure: ${ex.message}"
        developerMessage = ex.message ?: "Validation error"
      }

      else -> {
        userMessage = "Validation failure"
        developerMessage = "Unknown validation error"
      }
    }

    val errorResponse = ErrorResponse(
      status = HttpStatus.BAD_REQUEST,
      userMessage = userMessage,
      developerMessage = developerMessage,
    )

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse).also {
      log.info("Validation exception: {}", ex.message)
    }
  }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.error("Unexpected exception", e) }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException::class)
  fun handleAccessDeniedException(ex: org.springframework.security.access.AccessDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(FORBIDDEN)
    .body(
      ErrorResponse(
        status = FORBIDDEN,
        userMessage = "Access denied",
        developerMessage = ex.message ?: "",
      ),
    )

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
