package com.example.timetoeat.global.auth.entity;

import com.example.timetoeat.global.auth.model.Role;
import com.example.timetoeat.global.auth.model.provider.Oauth2Provider;
import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "member")
@Entity
public class MemberEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    private int age;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    public static MemberEntity from(Oauth2Provider oauth2Provider, Role role) {
        return MemberEntity.builder()
                .username(oauth2Provider.getUsername())
                .email(oauth2Provider.getEmail())
                .role(role)
                .build();
    }
}
