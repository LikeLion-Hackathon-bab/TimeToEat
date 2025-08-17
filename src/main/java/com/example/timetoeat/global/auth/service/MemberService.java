package com.example.timetoeat.global.auth.service;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.entity.SocialAccount;
import com.example.timetoeat.global.auth.model.Role;
import com.example.timetoeat.global.auth.model.provider.Oauth2Provider;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.member.repository.SocialAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberService {
    private final MemberJpaRepository memberJpaRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Transactional
    public SocialAccount findByProviderId(Oauth2Provider oauth2Provider) {
        return socialAccountRepository.findByProviderId(oauth2Provider.getProviderId())
                .orElseGet(() -> registerByOAuth2(oauth2Provider));
    }

    @Transactional
    public SocialAccount registerByOAuth2(Oauth2Provider oauth2Provider) {
        Role role = GenerateRole();
        MemberEntity memberEntity = MemberEntity.from(oauth2Provider, role);
        memberJpaRepository.save(memberEntity);
        SocialAccount socialAccount = SocialAccount.from(oauth2Provider, memberEntity);
        return socialAccountRepository.save(socialAccount);
    }

    private Role GenerateRole() {
        return Role.ROLE_MEMBER;
    }

    public MemberEntity getById(Long memberId) {
        return memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다. ID: " + memberId));
    }
}
