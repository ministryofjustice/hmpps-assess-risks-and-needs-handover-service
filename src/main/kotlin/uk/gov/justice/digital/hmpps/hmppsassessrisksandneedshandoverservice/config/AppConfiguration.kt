package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("app")
class AppConfiguration {
  lateinit var services: Services
  lateinit var self: Self
  lateinit var clients: Map<String, Client>

  class Services {
    lateinit var hmppsAuth: Service
    lateinit var oasys: OasysService
    lateinit var coordinatorApi: Service

    open class Service {
      lateinit var baseUrl: String
    }

    class OasysService : Service() {
      lateinit var returnUrls: List<String>
    }
  }

  class Self {
    lateinit var baseUrl: String
    lateinit var externalUrl: String
    lateinit var endpoints: Endpoints

    class Endpoints {
      lateinit var handover: String
      lateinit var context: String
    }
  }
  class Client {
    lateinit var secret: String
    lateinit var oauthRedirectUris: ArrayList<String>
    lateinit var handoverRedirectUri: String
  }
}
