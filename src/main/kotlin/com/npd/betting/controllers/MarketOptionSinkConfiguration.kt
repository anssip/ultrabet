package com.npd.betting.controllers

import com.npd.betting.model.MarketOption
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class MarketOptionSinkConfiguration {
  @Bean
  fun marketOptionSink(): AccumulatingSink<MarketOption> {
    return AccumulatingSink<MarketOption>(20, java.time.Duration.ofSeconds(10))
  }
}