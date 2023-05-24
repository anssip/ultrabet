package com.npd.betting.controllers

import com.npd.betting.model.MarketOption
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Sinks


@Configuration
class MarketOptionSinkConfiguration {
  @Bean
  fun marketOptionSink(): Sinks.Many<MarketOption> {
    return Sinks.many().multicast().onBackpressureBuffer<MarketOption>()
  }
}