package com.example.monsterhunter.service;

import com.example.monsterhunter.entity.RefreshToken;
import com.example.monsterhunter.entity.User;
import com.example.monsterhunter.exception.InvalidTokenException;
import com.example.monsterhunter.exception.TokenExpiredException;
import com.example.monsterhunter.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional   // ⚠️ 必加！deleteByUser 這種衍生刪除方法需要交易，沒加的話「第二次登入」會爆 TransactionRequiredException
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final RefreshTokenRepository refreshTokenRepository;

    /** 建立並儲存 Refresh Token（每個使用者只保留一個，舊的先刪） */
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusMillis(refreshTokenExpiration));

        return refreshTokenRepository.save(token);
    }

    /** 驗證 Refresh Token（不存在 → InvalidTokenException；過期 → 刪除並丟 TokenExpiredException） */
    public RefreshToken verifyExpiration(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh Token 無效"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenExpiredException("Refresh Token 已過期，請重新登入");
        }

        return refreshToken;
    }

    /** 撤銷使用者的所有 Refresh Token（登出用） */
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
