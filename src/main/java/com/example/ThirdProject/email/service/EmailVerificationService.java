package com.example.ThirdProject.email.service;

import com.example.ThirdProject.common.exception.*;
import com.example.ThirdProject.email.dto.EmailPurpose;
import com.example.ThirdProject.email.util.EmailSender;
import com.example.ThirdProject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final EmailSender mailSender;
    private final Random random = new SecureRandom();

    @Value("${email.verification.code-length}")
    private int codeLength;

    @Value("${email.verification.code-ttl:5}")
    private long codeTtlMinutes;

    @Value("${email.verification.verified-ttl:15}")
    private long verifiedTtlMinutes;

    private static final String VERIFIED_KEY_PREFIX = "ev:verified";

    private static final String CODE_KEY_PREFIX = "ev:code";


    // ===================== 인증 코드 발송 =====================
    public void sendVerificationCode(String email, EmailPurpose purpose) {
        // 인증 코드 생성
        String verificationCode = generateCode();
        String codeKey = generateRedisKey(CODE_KEY_PREFIX, purpose, email);

        // Redis에 저장 (TTL)
        redis.opsForValue().set(codeKey, verificationCode, Duration.ofMinutes(codeTtlMinutes));

        // 이메일 제목 설정
        String emailSubject = "[FilterFacts] " + purpose + " 이메일 인증 코드";

        // 이메일 본문 설정
        String emailBody = """
        안녕하세요!
        
        요청하신 인증 코드는 %s 입니다.
        %d분 내에 입력해주세요.
        """.formatted(verificationCode, codeTtlMinutes);


        // 이메일 전송
        mailSender.send(email, emailSubject, emailBody);
    }

    // ===================== 인증 코드 검증 =====================
    public void verify(String email, EmailPurpose purpose, String code) {
        String codeKey = generateRedisKey(CODE_KEY_PREFIX, purpose, email);

        String saved = redis.opsForValue().get(codeKey);
        if (saved == null || !saved.equals(code)) {
            throw new InvalidVerificationCodeException();
        }

        // 사용 후 코드 삭제
        redis.delete(codeKey);

        // 인증 완료 마크 (TTL)
        String verifiedKey = generateRedisKey(VERIFIED_KEY_PREFIX, purpose, email);
        redis.opsForValue().set(verifiedKey, "1", Duration.ofMinutes(verifiedTtlMinutes));

        // 코드 검증 성공 후, 회원가입 목적(signup)일 경우에만 DB 중복 체크
        if (purpose == EmailPurpose.LINK || purpose == EmailPurpose.SIGNUP) {
            if (userRepository.findByEmail(email).isPresent()) {
                // 이메일이 이미 DB에 존재하면, 일반적인 인증 성공이 아니라 연동이 필요함을 알립니다.
                throw new EmailDuplicateForLinkingException();
            }
        }
    }

    // ===================== 인증 상태 확인 =====================
    public void ensureVerified(String email, EmailPurpose purpose) {
        String verifiedKey = generateRedisKey(VERIFIED_KEY_PREFIX, purpose, email);

        String v = redis.opsForValue().get(verifiedKey);
        if (v == null) {
            throw new EmailNotVerifiedException();
        }
        redis.delete(verifiedKey);
    }

    // ===================== 인증 코드 생성 =====================
    private String generateCode() {
        int max = (int) Math.pow(10, codeLength);
        return String.format("%0" + codeLength + "d", random.nextInt(max));
    }

    private String generateRedisKey(String prefix, EmailPurpose purpose, String email) {
        return prefix + ":" + purpose.toString().toLowerCase() + ":" + email;
    }
}


