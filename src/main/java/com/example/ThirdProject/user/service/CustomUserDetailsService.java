package com.example.ThirdProject.user.service;

import com.example.ThirdProject.user.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = users.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // 권한 없이(빈 리스트) 반환. 비밀번호는 DB에 저장된 해시 그대로.
        return User
                .withUsername(user.getId())
                .password(user.getPassword())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)            // 필요하면 엔티티 필드로 제어
                .build();
    }
}