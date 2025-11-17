package com.example.ThirdProject.common.oauth;

import com.example.ThirdProject.common.exception.AccountNeedsLinkingException;
import com.example.ThirdProject.common.exception.AdditionalInfoRequiredException;
import com.example.ThirdProject.user.entity.User;
import com.example.ThirdProject.user.entity.UserProvider;
import com.example.ThirdProject.user.repository.UserProviderRepository;
import com.example.ThirdProject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2AccessToken accessToken = userRequest.getAccessToken();

        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();


        // 1. JSON 파싱
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        // 2. Provider ID 기반으로 이미 로그인한 유저인지 먼저 확인
        Optional<UserProvider> linkedProvider =
                userProviderRepository.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId());

        User user;
        if (linkedProvider.isPresent()) {
            // 🚩 Case 1: 연동된 유저 발견 (가장 깔끔한 성공, 이메일 유무 상관 없음)
            user = linkedProvider.get().getUser(); // FK를 통해 코어 User 객체 획득
//            user = attributes.update(user); // 프로필 업데이트
        } else if (attributes.getEmail() != null) {
            // 🚩 Case 2: Provider 연동은 없지만, 이메일은 제공됨. (계정 통합 및 신규 가입 플로우)

            Optional<User> userByEmail = userRepository.findByEmail(attributes.getEmail());

            if (userByEmail.isPresent()) {
                // Case 2-A: Email이 DB에 있음 -> 연동이 필요함 (Link Confirmation Flow)
                user = userByEmail.get();

                OAuth2FailureData dataPayload = new OAuth2FailureData(
                        attributes.getProviderId(), attributes.getProvider(), attributes.getName(),
                        attributes.getPicture(), user.getEmail() // 기존 유저 이메일
                );

                throw new AccountNeedsLinkingException(dataPayload, "기존 계정이 존재합니다. 연동 확인이 필요합니다.");

            } else {
                // Case 2-B: Email이 DB에 없음 -> 신규 회원 가입
                user = attributes.toEntity();
                userRepository.save(user); // 코어 User 저장

                // UserProvider 링크 생성
                userProviderRepository.save(
                        UserProvider.builder().user(user).provider(attributes.getProvider()).providerId(attributes.getProviderId()).build()
                );
            }

        } else {
            // 🚩 Case 3: Provider 연동도 없고, 이메일도 없음 (최초 이메일 입력 강제)

            OAuth2FailureData dataPayload = new OAuth2FailureData(
                    attributes.getProviderId(), attributes.getProvider(), attributes.getName(),
                    attributes.getPicture(), null // Email is null
            );

            // Additional Info Required Flow
            throw new AdditionalInfoRequiredException(dataPayload);
        }

        // 최종 User 엔티티 업데이트 (프로필 업데이트나 신규 저장)
        userRepository.save(user);

        // 4. 인증 객체 반환 (SuccessHandler 실행)
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }
}