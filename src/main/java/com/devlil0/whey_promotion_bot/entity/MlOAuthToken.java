package com.devlil0.whey_promotion_bot.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ml_oauth_token")
public class MlOAuthToken {

    @Id
    private Long id = 1L;

    @Column(name = "access_token", length = 1000, nullable = false)
    private String accessToken;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public MlOAuthToken() {}

    public Long getId() { return id; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
