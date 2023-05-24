package com.npd.betting.importer

import com.npd.betting.model.MarketOption
import org.springframework.context.ApplicationEvent

class MarketOptionUpdatedEvent(source: Any, private val updatedMarketOptions: List<MarketOption>) :
  ApplicationEvent(source) {
    
  fun getUpdatedMarketOptions(): List<MarketOption> {
    return updatedMarketOptions
  }
}





