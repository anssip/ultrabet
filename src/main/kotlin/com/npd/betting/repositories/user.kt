package com.npd.betting.repositories

import com.npd.betting.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Int> {
    // Custom methods can be added here if needed
}
