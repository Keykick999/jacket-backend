package com.example.ThirdProject.user.service;

import com.example.ThirdProject.user.entity.User;
import com.example.ThirdProject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 회원 정보 조회
        User user = userRepository.findById(username).orElseThrow(() -> new UsernameNotFoundException(username));

        // UserDetails 반환
        return org.springframework.security.core.userdetails.User
                .builder()
                .username(username)
                .password(user.getPassword())
                .build();
    }
}