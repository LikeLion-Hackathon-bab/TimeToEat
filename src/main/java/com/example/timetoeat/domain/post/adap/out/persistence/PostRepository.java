package com.example.timetoeat.domain.post.adap.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<PostEntity, Long> {
    @Query("SELECT p FROM PostEntity p JOIN FETCH p.member")
    List<PostEntity> findAllWithMember();
}
