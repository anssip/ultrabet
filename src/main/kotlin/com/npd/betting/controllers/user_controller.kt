package com.npd.betting.controllers

import com.npd.betting.model.User
import com.npd.betting.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class UserController @Autowired constructor(private val userRepository: UserRepository) {

    @SchemaMapping(typeName = "Query", field = "getUser")
    fun getUser(@Argument id: Int): User {
        println("getUser")
        return userRepository.findById(id).orElse(null)
    }

//    @SchemaMapping
//    fun author(book: Book): Author? {
//        return Author.getById(book.authorId())
//    }
}