package com.devlil0.whey_promotion_bot.repository;

import com.devlil0.whey_promotion_bot.entity.MlOAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MlOAuthTokenRepository extends JpaRepository<MlOAuthToken, Long> {}
