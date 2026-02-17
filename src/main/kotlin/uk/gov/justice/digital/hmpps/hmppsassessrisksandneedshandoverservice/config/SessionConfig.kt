package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession
import org.springframework.session.web.http.CookieSerializer
import org.springframework.session.web.http.DefaultCookieSerializer

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 12 * 60 * 60)
class SessionConfig {

  @Bean
  fun cookieSerializer(
    @Value("\${server.servlet.session.cookie.name:HMPPS_ARNS_HANDOVER_SESSION}") cookieName: String,
    @Value("\${server.servlet.session.cookie.same-site:Lax}") sameSite: String,
  ): CookieSerializer {
    val serializer = DefaultCookieSerializer()
    serializer.setCookieName(cookieName)
    serializer.setSameSite(sameSite.replaceFirstChar { it.uppercase() })
    serializer.setUseHttpOnlyCookie(true)
    serializer.setCookiePath("/")
    return serializer
  }
}
