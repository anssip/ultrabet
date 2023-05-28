package com.npd.betting.controllers

import com.npd.betting.model.Event
import com.npd.betting.model.MarketOption
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class SinksConfiguration {
  @Bean
  fun marketOptionSink(): AccumulatingSink<MarketOption> {
    return AccumulatingSink<MarketOption>(20, java.time.Duration.ofSeconds(10))
  }

  @Bean
  fun scoreUpdatesSink(): AccumulatingSink<Event> {
    return AccumulatingSink<Event>(20, java.time.Duration.ofSeconds(1))
  }

  @Bean
  fun eventStatusUpdatesSink(): AccumulatingSink<Event> {
    return AccumulatingSink<Event>(20, java.time.Duration.ofSeconds(1))
  }

}
