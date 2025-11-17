package com.example.ThirdProject.common.oauth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 이메일 미제공 시 추가 가입 페이지로 전달할 필수 데이터를 담는 DTO
 */
@Getter
@AllArgsConstructor
@ToString
public class OAuth2FailureData implements Serializable {
    private String providerId;
    private String provider;
    private String nickname;
    private String picture; // 프로필 이미지 경로
    private String existingEmail; // DB에서 찾은 기존 사용자의 이메일을 담기 위한 필드

    public OAuth2FailureData(String providerId, String provider, String nickname, String picture) {
        this.providerId = providerId;
        this.provider = provider;
        this.nickname = nickname;
        this.picture = picture;
    }
}