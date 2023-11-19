package com.npd.betting.controllers

import com.npd.betting.model.Bet
import com.npd.betting.model.User
import com.npd.betting.model.Wallet
import com.npd.betting.repositories.UserRepository
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import java.math.BigDecimal

@Controller
class UserController @Autowired constructor(
    private val userRepository: UserRepository,
    private val entityManager: EntityManager
) {

    @SchemaMapping(typeName = "Query", field = "getUser")
    fun getUser(@Argument id: Int): User {
        return userRepository.findById(id).orElse(null)
    }

    @SchemaMapping(typeName = "User", field = "bets")
    fun getUserBets(user: User): List<Bet> {
        val query = entityManager.createQuery(
            "SELECT u FROM User u JOIN FETCH u.bets WHERE u.id = :id", User::class.java
        )
        query.setParameter("id", user.id)
        val resultList = query.resultList
        return if (resultList.isEmpty()) emptyList() else resultList[0].bets
    }

    @MutationMapping
    fun createUser(@Argument externalId: String, @Argument username: String? = null, @Argument email: String?): User {
        val user = User(username = username, email = email, externalId = externalId)
        val wallet = Wallet(user = user, balance = BigDecimal.ZERO)
        user.wallet = wallet
        userRepository.save(user)
        return user
    }
}