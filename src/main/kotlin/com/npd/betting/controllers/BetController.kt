package com.npd.betting.controllers

import com.npd.betting.model.Bet
import com.npd.betting.repositories.BetRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class BetController @Autowired constructor(
    private val betRepository: BetRepository
) {

    @SchemaMapping(typeName = "Query", field = "getBet")
    fun getBet(@Argument id: Int): Bet {
        return betRepository.findById(id).orElse(null)
    }

    @SchemaMapping(typeName = "Query", field = "listBets")
    fun listBets(@Argument userId: Int): List<Bet> {
        return betRepository.findByUserId(userId)
    }

}
