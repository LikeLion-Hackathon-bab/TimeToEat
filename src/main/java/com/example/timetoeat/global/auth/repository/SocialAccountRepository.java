package com.example.timetoeat.global.auth.repository;

import com.example.timetoeat.global.auth.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderId(String providerId);
}
