package com.example.ThirdProject.user.service;

import com.example.ThirdProject.common.exception.*;
import com.example.ThirdProject.common.jwt.JwtService;
import com.example.ThirdProject.common.jwt.TokenStore;
import com.example.ThirdProject.email.service.EmailVerificationService;
import com.example.ThirdProject.user.entity.User;
import com.example.ThirdProject.user.entity.UserProvider;
import com.example.ThirdProject.user.repository.UserProviderRepository;
import com.example.ThirdProject.user.repository.UserRepository;
import com.example.ThirdProject.user.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.authentication.LockedException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final TokenStore store;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final UserProviderRepository userProviderRepository;
    private final StringRedisTemplate redisTemplate;
    private static final String FAIL_KEY_PREFIX = "login:fail:";
    private static final long FAIL_LIMIT = 5;
    private static final long LOCK_TIME = 30; // 30분 차단

    /**
     * 회원가입
     */
    public String register(RegisterRequest req) {
        checkDuplicateUserId(req.userId());
        checkDuplicateEmail(req.email());
        emailVerificationService.ensureVerified(req.email(), "signup");

        User u = createUser(req);
        userRepository.save(u);

        return u.getId();
    }

    /**
     * 로그인
     */
    public TokenResponse login(LoginRequest req) throws LockedException {
        User user = userRepository.findByLoginId(req.username())
                .orElseThrow(() -> new UsernameNotFoundException("아이디 또는 비밀번호가 틀렸습니다."));

        String key = FAIL_KEY_PREFIX + req.username();

        // 로그인 실패 횟수 제한을
        String countStr = redisTemplate.opsForValue().get(key); // 현재 카운트 확인
        if (countStr != null && Integer.parseInt(countStr) >= FAIL_LIMIT) {
            // 5회 넘었으면 TTL(남은 시간) 확인해서 예외 던지기
            Long expire = redisTemplate.getExpire(key, TimeUnit.MINUTES);
            throw new CustomException(
                    ErrorCode.ACCOUNT_LOCKED,
                    "계정이 잠겼습니다. " + expire + "분 뒤에 다시 시도하세요."
            );
        }

        // 비밀번호 검증
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getId(), req.password())
            );

            // 4. [성공 처리] 실패 카운트 삭제
            redisTemplate.delete(key);
            UserDetails userDetails = (UserDetails) auth.getPrincipal();

            return generateTokens(userDetails);
        } catch (BadCredentialsException e) {
            handleLoginFail(key);
            throw new BadCredentialsException("비밀번호가 틀렸습니다.");
        }
    }

    /**
     * 토큰 재발급
     */
    public TokenResponse refresh(String refresh) {
        String username = jwtService.getUserId(refresh);
        if (!store.isRefreshValid(username, refresh)) {
            throw new InvalidRefreshTokenException();
        }

        store.revokeRefresh(username, refresh);
        UserDetails stub = org.springframework.security.core.userdetails.User
                .withUsername(username).password("N/A").build();

        return generateTokens(stub);
    }

    /**
     * 로그아웃
     */
    public void logout(LogoutRequest req) {
        String refresh = req.refreshToken();
        validateRefreshToken(refresh);

        String username = jwtService.getUserId(refresh);
        store.revokeRefresh(username, refresh);
    }

    // ========================= 공용 재사용 메서드 =========================

    public User findUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    // ========================= private 헬퍼 메서드 =========================
    private void handleLoginFail(String key) {
        // INCR 명령어 (없으면 1로 생성됨)
        Long count = redisTemplate.opsForValue().increment(key);

        // 첫 실패면 유효시간(TTL) 설정 (예: 5번 시도를 10분 동안만 기억)
        if (count != null && count == 1) {
            redisTemplate.expire(key, 10, TimeUnit.MINUTES);
        }

        // 5회 도달 시 -> 락 타임(30분)으로 연장해서 못 들어오게 막음
        if (count != null && count == FAIL_LIMIT) {
            redisTemplate.expire(key, LOCK_TIME, TimeUnit.MINUTES);
        }
    }

    private User createUser(RegisterRequest req) {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .loginId(req.userId())
                .nickname(req.nickname())
                .password(passwordEncoder.encode(req.password()))
                .email(req.email())
                .phone(req.phone())
                .name(req.userName())
                .build();
    }

    private TokenResponse generateTokens(UserDetails user) {
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        long expMs = jwtService.getExpiration(access).getTime() - System.currentTimeMillis();
        long refreshTtl = jwtService.getExpiration(refresh).getTime() - System.currentTimeMillis();
        store.saveRefresh(user.getUsername(), refresh, refreshTtl);

        return new TokenResponse(access, expMs, refresh);
    }

    private void validateRefreshToken(String refresh) {
        if (!jwtService.isTokenValid(refresh) || !jwtService.isRefresh(refresh)) {
            throw new InvalidRefreshTokenException();
        }
    }

    // ========================= private 단일 책임 헬퍼 (검증) =========================

    private void checkDuplicateUserId(String userId) {
        if (userRepository.existsByLoginId((userId))) {
            throw new DuplicateUserIdException(userId);
        }
    }

    private void checkDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
    }

    /**
     * 이메일 미제공 소셜 유저의 최종 회원가입을 완료합니다.
     */
    @Transactional
    public User completeSocialRegistration(SocialCompleteRequest request) {
        // 1. 이메일 중복 검사 (혹시 다른 유저가 이 이메일로 이미 가입했을 경우)
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            throw new EmailAlreadyInUseException();
        }

        // 2. User 엔티티 생성 (최종 저장)
        // 이 로직은 OAuthAttributes.toEntity()와 유사하지만, 이메일을 수동으로 받습니다.
        User newUser = User.builder()
                .email(request.getEmail())
                .nickname(request.getNickname())
                .name(request.getNickname()) // 이름은 닉네임과 동일하게 설정 (정책에 따라 변경 가능)
                .isPrivate(false)
                .introduction("자기소개가 아직 없습니다.")
                .profileImagePath("/image/UserImageDefault.png")
                .isPrivate(false)
                .build();

        // 3. DB 저장
        User saved = userRepository.save(newUser);
        UserProvider provider = UserProvider.builder()
                .user(saved)
                .provider(request.getProvider())
                .providerId(request.getProviderId())
                .build();
        userProviderRepository.save(provider);
        return saved;
    }

    /**
     * 계정 연동을 수행하고, 성공 시 JWT를 발급합니다.
     * @param request 연동 요청 DTO
     * @return 발급된 JWT와 성공 메시지
     */
    public UserLinkResponse linkAccount(UserLinkRequest request) {
        // 1. 기존 유저를 이메일로 찾습니다.
        User existingUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "기존 계정을 찾을 수 없습니다.")
                );

        // 2. 해당 Provider가 이미 연동되어 있는지 최종 확인 (이중 체크)
        Optional<UserProvider> existingLink = userProviderRepository
                .findByUserAndProvider(existingUser, request.getProvider());

        if (existingLink.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미 해당 Provider와 연동된 계정입니다.");
        }

        // 3. UserProvider 연동 정보 생성 및 저장
        UserProvider newProviderLink = UserProvider.builder()
                .user(existingUser) // FK 설정
                .provider(request.getProvider())
                .providerId(request.getProviderId())
                .build();

        userProviderRepository.save(newProviderLink);

        // 4. 연동 성공 후, 해당 유저의 JWT 토큰을 새로 발급하여 즉시 로그인 처리
        String newAccessToken = jwtService.generateAccessToken(existingUser.getId());

        return UserLinkResponse.builder()
                .message("계정 연동이 완료되었습니다.")
                .accessToken(newAccessToken)
                .userUuid(existingUser.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public void checkUserIdAvailability(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new InvalidRequestException("아이디를 입력해주세요.");
        }

        if (userRepository.existsByLoginId(userId)) {
            throw new UserIdDuplicateException();
        }
        // 중복이 아니면 아무것도 하지 않음 (성공 처리)
    }
}
