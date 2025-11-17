package com.example.ThirdProject.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID; // UUID를 import 합니다.
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "Users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * uuid가 담김
     */
    @Id
    @Column(name = "id", nullable = false, unique = true, updatable = false)
    private String id;

    /**
     * 자체 회원가입 유저만 이 값을 사용하며, 소셜 로그인 유저는 이 값이 null입니다.
     */
    @Column(name = "login_id", length = 50, unique = true) // nullable = true (기본값)
    private String loginId; // 자체 로그인 ID

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    /**
     * 소셜 로그인 유저는 이 값이 null이거나, OAuthAttributes에서 채워준 임의의 값입니다.
     */
    @Column(name = "password", nullable = true, length = 255)
    private String password;

    /**
     * 이메일은 모든 유저(자체, 소셜)를 식별하는 고유 키입니다.
     * (nullable = false, unique = true 유지)
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "introduction", length = 1000)
    private String introduction;

    @Column(name = "profile_image_path")
    private String profileImagePath;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 새 엔티티가 저장되기 직전에 UUID를 생성합니다.
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}