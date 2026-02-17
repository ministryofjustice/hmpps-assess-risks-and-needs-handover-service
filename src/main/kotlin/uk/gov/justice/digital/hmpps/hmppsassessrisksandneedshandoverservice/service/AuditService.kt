package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.events.AuditEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Instant

@Service
class AuditService(
  private val hmppsQueueService: HmppsQueueService,
  private val jsonMapper: JsonMapper,
  private val appConfiguration: AppConfiguration,
) {
  private val auditQueue by lazy {
    hmppsQueueService.findByQueueId("audit") ?: throw RuntimeException("Queue with ID 'audit' does not exist")
  }
  private val sqsClient by lazy { auditQueue.sqsClient }
  private val queueUrl by lazy { auditQueue.queueUrl }

  fun publish(event: AuditEvent, who: String, subjectId: String?, subjectType: String = "CRN") {
    val message = AuditMessage(
      what = event.name,
      who = who,
      service = appConfiguration.name,
      subjectId = subjectId,
      subjectType = subjectType,
    )

    log.info("Sending audit event ${message.what} for ${message.who}")
    sqsClient.sendMessage {
      it.queueUrl(queueUrl)
        .messageBody(jsonMapper.writeValueAsString(message))
        .build()
    }.whenComplete { _, error ->
      if (error != null) {
        log.error("Failed to send audit event ${message.what} for ${message.who}", error)
      }
    }
  }

  private companion object {
    private val log = LoggerFactory.getLogger(AuditService::class.java)
  }
}

private data class AuditMessage(
  val what: String,
  val who: String,
  val service: String,
  val subjectId: String?,
  val subjectType: String,
  val `when`: Instant = Instant.now(),
)
