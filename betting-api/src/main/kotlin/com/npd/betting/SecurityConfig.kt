package com.npd.betting

import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest
import org.springframework.boot.autoconfigure.security.reactive.PathRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer
import org.springframework.security.web.SecurityFilterChain


/**
 * Configures our application with Spring Security to restrict access to our API endpoints.
 */
@Configuration
open class SecurityConfig {
  @Bean
  @Throws(Exception::class)
  open fun filterChain(http: HttpSecurity): SecurityFilterChain {
    return http
      .authorizeHttpRequests(
        Customizer { authorize ->
          authorize
            .requestMatchers("/private/actuator/health").permitAll()
            .requestMatchers("/private/graphql").authenticated()
        }
      )
      .cors(Customizer.withDefaults())
      .oauth2ResourceServer { oauth2: OAuth2ResourceServerConfigurer<HttpSecurity?> ->
        oauth2
          .jwt(Customizer.withDefaults())
      }
      .build()
  }
}