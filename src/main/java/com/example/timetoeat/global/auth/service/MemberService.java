package com.example.timetoeat.global.auth.service;

import com.example.timetoeat.global.auth.entity.Member;
import com.example.timetoeat.global.auth.entity.SocialAccount;
import com.example.timetoeat.global.auth.model.Role;
import com.example.timetoeat.global.auth.model.provider.Oauth2Provider;
import com.example.timetoeat.global.auth.repository.MemberRepository;
import com.example.timetoeat.global.auth.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Transactional
    public SocialAccount findByRegistrationIdOrRegister(Oauth2Provider oauth2Provider) {
        return socialAccountRepository.findByRegistrationId(oauth2Provider.getRegistrationId())
                .orElseGet(() -> registerByOAuth2(oauth2Provider));
    }

    @Transactional
    public SocialAccount registerByOAuth2(Oauth2Provider oauth2Provider) {
        Role role = GenerateRole();
        Member member = Member.from(oauth2Provider, role);
        memberRepository.save(member);
        SocialAccount socialAccount = SocialAccount.from(oauth2Provider,member);
        return socialAccountRepository.save(socialAccount);
    }

    private Role GenerateRole() {
        return Role.ROLE_MEMBER;
    }

    public Member getById(Long memberId) {
        // 예외 처리 필요시 진행!
        return memberRepository.findById(memberId).orElse(null);
    }
}
