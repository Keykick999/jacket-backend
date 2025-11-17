//package com.example.ThirdProject.user;
//
//
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//
//@Service
//@RequiredArgsConstructor
//public class UserService {
//
//    private final UserRepository userRepository;
//    private final UserProviderRepository userProviderRepository;
//    private final TermRepository termRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final JwtTokenProvider jwtTokenProvider;
//    private final AuthenticationManager authenticationManager;
//    private final EmailVerificationService emailVerificationService;
//    private final UserTermService userTermService;
//
//    @Transactional
//    public void register(UserJoinRequest request) {
//        if (userRepository.existsById(request.getUserId())) {
//            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
//        }
//
//        // 이메일 인증 코드 검증
//        emailVerificationService.verifyAndMarkAsVerified(
//                request.getEmail(),
//                request.getVerificationCode(),
//                "signup"
//        );
//
//        // 약관 null 방어
//        List<UserTermAgreementRequest> agreements = request.getAgreements();
//        if (agreements == null || agreements.isEmpty()) {
//            throw new IllegalArgumentException("약관 동의 정보가 없습니다.");
//        }
//
//        // 필수 약관 체크 검증
//        List<Term> requiredTerms = termRepository.findByIsRequiredTrue();
//
//        Set<String> requiredCodes = requiredTerms.stream()
//                .map(term -> term.getTermCode() != null ? term.getTermCode().toUpperCase() : "null")
//                .collect(Collectors.toSet());
//
//        Set<String> agreedCodes = agreements.stream()
//                .filter(dto -> dto.isAgreed() && dto.getTermCode() != null)
//                .map(dto -> dto.getTermCode().toUpperCase())
//                .collect(Collectors.toSet());
//        // === 로그 출력 끝 ===
//
//        if (!agreedCodes.containsAll(requiredCodes)) {
//            throw new IllegalArgumentException("모든 필수 약관에 동의해야 합니다.");
//        }
//
//        // 회원 가입 코드 일부
//        User user = User.builder()
//                .userId(request.getUserId())
//                .nickname(request.getNickname())
//                .password(passwordEncoder.encode(request.getPassword()))
//                .email(request.getEmail())
//                .phone(request.getPhone())
//                .name(request.getName())
//                .introduction("자기소개가 아직 없습니다.")
//                .profileImagePath("/image/UserImageDefault.png")
//                .build();
//
//        userRepository.save(user);
//        userTermService.saveUserAgreements(user.getUserId(), agreements);
//    }
//
//
//    // 로그인 처리
//    public UserLoginResponse login(UserLoginRequest request) {
//        try {
//            authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword())
//            );
//        } catch (Exception e) {
//            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
//        }
//
//        // 로그인 성공 시, DB에서 User 정보를 조회
//        User user = userRepository.findById(request.getUserId())
//                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다.")); // (인증이 통과했으므로 이 오류는 거의 발생 안 함)
//
//        // 'userId' 대신 'userUuid'를 담아서 생성
//        String token = jwtTokenProvider.createToken(user.getUserUuid());
//
//        return new UserLoginResponse(token);
//    }
//
//
//    // 회원 정보 조회
//    public UserResponse getUserInfo(String userUuid) {
//        User user = userRepository.findByUserUuid(userUuid)
//                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
//
//        return new UserResponse(
//                user.getUserUuid(),
//                user.getNickname(),
//                user.getEmail(),
//                user.getPhone(),
//                user.getName(),
//                user.getCreatedAt(),
//                user.getIntroduction(),
//                user.getProfileImagePath()
//        );
//    }
//
//    // 회원 정보 수정
//    @Transactional
//    public void updateUserInfo(String userId, UserUpdateRequest request) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
//
//        user.setNickname(request.getNickname());
//        user.setPhone(request.getPhone());
//
//        user.setIntroduction(request.getIntroduction());
//        user.setProfileImagePath(request.getProfileImagePath());
//
//        userRepository.save(user);
//    }
//
//    //회원 정보 수정 - 유저 업로드 이미지 기준
//    @Transactional
//    public void updateUserProfileWithImage(String userId, String nickname, String phone, String introduction,
//                                           MultipartFile profileImage, String defaultImage) {
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
//
//        user.setNickname(nickname);
//        user.setPhone(phone);
//        user.setIntroduction(introduction);
//
//        String imagePath = saveImage(profileImage);
//
//        if (imagePath != null) {
//            user.setProfileImagePath(imagePath); // 사용자 이미지 경로 저장
//        } else if (defaultImage != null) {
//            user.setProfileImagePath(defaultImage); // 기본 이미지 저장
//        }
//
//        userRepository.save(user);
//    }
//
//    // 이미지 저장
//    public String saveImage(MultipartFile imageFile) {
//        // 1. 저장할 실제 경로
//        Path path = Paths.get("C:", "uploads"); // 경로 변경: "C:/uploads"
//        String uploadDir = path.toString();
//
//        String storedFileName = null;
//
//        if (imageFile != null && !imageFile.isEmpty()) {
//            try {
//                // 2. 파일명 처리
//                String originalFileName = imageFile.getOriginalFilename();
//                String ext = originalFileName.substring(originalFileName.lastIndexOf("."));
//                storedFileName = UUID.randomUUID().toString() + ext;
//
//                // 3. 실제 저장
//                File dest = new File(uploadDir, storedFileName);
//                if (!dest.getParentFile().exists()) {
//                    dest.getParentFile().mkdirs();
//                }
//                imageFile.transferTo(dest);
//
//
//                // DB에는 상대 경로로 저장
//                return "/uploads/" + storedFileName;
//
//            } catch (IOException e) {
//                throw new RuntimeException("이미지 저장 중 오류 발생", e);
//            }
//        }
//
//        return null;
//    }
//
//
//    // 아이디 찾기
//    public String findIdByEmail(String email) {
//        return userRepository.findIdByEmail(email)
//                .map(User::getUserId)
//                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 가입된 계정이 없습니다."));
//    }
//
//
//    //비밀번호 초기화
//    @Transactional
//    public void resetPassword(String email, String newPassword, String verificationCode) {
//        // 1. 인증 코드 검증
//        emailVerificationService.verifyAndMarkAsVerified(email, verificationCode, "reset");
//
//        // 2. 유저 조회
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new IllegalArgumentException("해당하는 사용자가 없습니다."));
//
//        // 반드시 newPassword를 암호화해서 저장해야 함
//        user.setPassword(passwordEncoder.encode(newPassword));
//
//        userRepository.save(user);
//    }
//
//
//
//
//
//    // 요청보낸 회원의 id값 확인
//    public String getCurrentUserId() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        return authentication.getName();  // JWT에서 setSubject(userId)로 설정했던 값
//    }
//
//    public User getAuthenticatedUser() {
//        String userUuid = getCurrentUserId();
//        return userRepository.findById(userUuid)
//                .orElseThrow(() -> new UnauthenticatedUserException("인증된 사용자가 아닙니다."));
//    }
//
//    public Optional<User> getUserIfAuthenticated() {
//        String userId = getCurrentUserId();
//        return userRepository.findById(userId);
//    }
//
//    public Optional<User> findById(String userId) {
//        return userRepository.findByUserUuid(userId);
//    }
//
//    /**
//     * [신규] 이메일 미제공 소셜 유저의 최종 회원가입을 완료합니다.
//     */
//    @Transactional
//    public User completeSocialRegistration(SocialCompleteRequest request) {
//        // 1. 이메일 중복 검사 (혹시 다른 유저가 이 이메일로 이미 가입했을 경우)
//        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
//        if (existingUser.isPresent()) {
//            throw new EmailAlreadyInUseException("이미 사용 중인 이메일 주소입니다. 다른 계정으로 로그인해주세요.");
//        }
//
//        // 2. User 엔티티 생성 (최종 저장)
//        // 이 로직은 OAuthAttributes.toEntity()와 유사하지만, 이메일을 수동으로 받습니다.
//        User newUser = User.builder()
//                .email(request.getEmail())
//                .nickname(request.getNickname())
//                .name(request.getNickname()) // 이름은 닉네임과 동일하게 설정 (정책에 따라 변경 가능)
//                .isPrivate(false)
//                .introduction("자기소개가 아직 없습니다.")
//                .profileImagePath("/image/UserImageDefault.png")
//                .isPrivate(false)
//                .build();
//
//        // 3. DB 저장
//        User saved = userRepository.save(newUser);
//        UserProvider provider = UserProvider.builder()
//                .user(saved)
//                .provider(request.getProvider())
//                .providerId(request.getProviderId())
//                .build();
//        userProviderRepository.save(provider);
//        return saved;
//    }
//
//    /**
//     * [신규] 계정 연동을 수행하고, 성공 시 JWT를 발급합니다.
//     * @param request 연동 요청 DTO
//     * @return 발급된 JWT와 성공 메시지
//     */
//    public UserLinkResponse linkAccount(UserLinkRequest request) {
//        // 1. 기존 유저를 이메일로 찾습니다.
//        User existingUser = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new ResponseStatusException(
//                        HttpStatus.NOT_FOUND,
//                        "기존 계정을 찾을 수 없습니다.")
//                );
//
//        // 2. 해당 Provider가 이미 연동되어 있는지 최종 확인 (이중 체크)
//        Optional<UserProvider> existingLink = userProviderRepository
//                .findByUserAndProvider(existingUser, request.getProvider());
//
//        if (existingLink.isPresent()) {
//            throw new ResponseStatusException(
//                    HttpStatus.BAD_REQUEST,
//                    "이미 해당 Provider와 연동된 계정입니다.");
//        }
//
//        // 3. UserProvider 연동 정보 생성 및 저장
//        UserProvider newProviderLink = UserProvider.builder()
//                .user(existingUser) // FK 설정
//                .provider(request.getProvider())
//                .providerId(request.getProviderId())
//                .build();
//
//        userProviderRepository.save(newProviderLink);
//
//        // 4. 연동 성공 후, 해당 유저의 JWT 토큰을 새로 발급하여 즉시 로그인 처리
//        String newAccessToken = jwtTokenProvider.createToken(existingUser.getUserUuid());
//
//        return UserLinkResponse.builder()
//                .message("계정 연동이 완료되었습니다.")
//                .accessToken(newAccessToken)
//                .userUuid(existingUser.getUserUuid())
//                .build();
//    }
//}