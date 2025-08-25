package com.example.timetoeat.domain.member.repository;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findByEmail(String email);

    List<MemberEntity> findTop30ByIdNotOrderByCreatedAtDesc(Long id);
}
