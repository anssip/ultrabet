package com.npd.betting.controllers

import com.npd.betting.importer.MarketOptionUpdatedEvent
import com.npd.betting.model.MarketOption
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks

@Component
class MarketOptionUpdateListener(private val marketOptionSink: Sinks.Many<MarketOption>) :
  ApplicationListener<MarketOptionUpdatedEvent> {
  val logger: Logger = LoggerFactory.getLogger(MarketOptionUpdateListener::class.java)

  override fun onApplicationEvent(event: MarketOptionUpdatedEvent) {
    val updatedMarketOptions = event.getUpdatedMarketOptions()
    logger.info("Received ${updatedMarketOptions.size} updated market options")
    // Push the updated market options to the subscription clients
    updatedMarketOptions.forEach { marketOption ->
      marketOptionSink.emitNext(marketOption, Sinks.EmitFailureHandler.FAIL_FAST)
    }
  }
}
