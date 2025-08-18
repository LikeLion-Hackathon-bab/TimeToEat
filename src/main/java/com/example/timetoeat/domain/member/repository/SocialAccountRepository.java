package com.example.timetoeat.domain.member.repository;

import com.example.timetoeat.domain.member.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderId(String providerId);
}
