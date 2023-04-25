package com.npd.betting.repositories

import com.npd.betting.model.User
import org.springframework.data.jpa.repository.JpaRepository

// default methods in JPA repiositories
// https://www.tutorialspoint.com/spring_boot_jpa/spring_boot_jpa_repository_methods.htm

interface UserRepository : JpaRepository<User, Int> {
    // Custom methods can be added here if needed
}

interface BetRepository : JpaRepository<User, Int> {
    // Custom methods can be added here if needed
}
