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
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class UserService @Autowired constructor(
  val userRepository: UserRepository,
  val walletRepository: WalletRepository,
  val userController: UserController
) {
  val logger: Logger = LoggerFactory.getLogger(EventImporter::class.java)

  fun findAuthenticatedUser(): User {
    val principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal() as Jwt;
    val email = principal.getClaimAsString("email")
    if (email == null || email == "") {
      throw RuntimeException("No email found in JWT")
    }
    val user = userRepository.findByEmail(email)
    if (user == null) {
      logger.info("creating new user for email ${email}");
      return createUser(principal)
    }
    logger.info("found user ${user.id}")
    return user
  }

  private fun createUser(principal: Jwt): User {
    val email = principal.getClaimAsString("email")
    val nickname = principal.getClaimAsString("nickname")
    val user = userController.createUser(nickname ?: email, email)

    // make sure he has money :-)
    val wallet = walletRepository.findByUserId(user.id)
    wallet!!.balance = BigDecimal("1000.00")
    walletRepository.save(wallet)

    return user
  }
}