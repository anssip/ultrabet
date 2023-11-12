package com.npd.betting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
open class BettingGraphqlApi {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      runApplication<BettingGraphqlApi>(*args)
    }
  }
}
