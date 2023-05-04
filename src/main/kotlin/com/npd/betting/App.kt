package com.npd.betting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Controller

@SpringBootApplication
@EnableScheduling
class BettingGraphqlApi {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<BettingGraphqlApi>(*args)
        }
    }
}


@Controller
class FooController {
    @QueryMapping
    fun hello(): String {
        return "hello world"
    }

}