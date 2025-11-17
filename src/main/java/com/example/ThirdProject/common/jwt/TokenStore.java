package com.example.ThirdProject.common.jwt;

public interface TokenStore {
    void saveRefresh(String username, String refreshToken, long ttlMs);
    boolean isRefreshValid(String username, String refreshToken);
    void revokeRefresh(String username, String refreshToken);
}