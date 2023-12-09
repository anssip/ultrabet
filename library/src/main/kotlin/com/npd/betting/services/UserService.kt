package com.npd.betting.services

import com.npd.betting.controllers.UserController
import com.npd.betting.model.User
import com.npd.betting.model.Wallet
import com.npd.betting.repositories.UserRepository
import com.npd.betting.repositories.WalletRepository
import com.npd.betting.services.importer.EventImporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class UserService @Autowired constructor(
  val userRepository: UserRepository,
  val walletRepository: WalletRepository,
) {
  val logger: Logger = LoggerFactory.getLogger(UserService::class.java)

  fun findAuthenticatedUser(): User {
    val principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal() as Jwt;
    val sub = principal.getClaimAsString("sub")

    val user = userRepository.findByExternalId(sub)
    if (user == null) {
      logger.info("creating new user for externalId ${sub}");
      return createUser(externalId = sub, balance = BigDecimal(1000))
    }
    logger.info("found user ${user.id}")
    return user
  }

  fun createUser(externalId: String, username: String? = null, email: String? = null, balance: BigDecimal): User {
    val user = User(username = username, email = email, externalId = externalId)
    val wallet = Wallet(user = user, balance = balance)
    user.wallet = wallet

    userRepository.save(user)
    walletRepository.save(wallet)

    return user
  }
}