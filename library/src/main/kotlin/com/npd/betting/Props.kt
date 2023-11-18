package com.npd.betting

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties("ultrabet")
@Component
class Props {
  private lateinit var oddsApiKey: String

  fun setOddsApiKey(oddsApiKey: String) {
    this.oddsApiKey = oddsApiKey
  }

  fun getOddsApiKey(): String {
    return oddsApiKey
  }
}