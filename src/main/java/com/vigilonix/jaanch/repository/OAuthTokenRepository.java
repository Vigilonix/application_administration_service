package com.vigilonix.jaanch.repository;

import com.vigilonix.jaanch.aop.Timed;
import com.vigilonix.jaanch.model.OAuthToken;
import com.vigilonix.jaanch.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthToken, String> {
    OAuthToken findByUserAndExpireTimeGreaterThan(User user, Long expireTime);

    OAuthToken findByToken(String token);

    @Timed
    OAuthToken findByTokenAndExpireTimeGreaterThan(String authToken, long currentTimeMillis);

    @Timed
    @Modifying
    @Query("delete from OAuthToken o where o.user = :user")
    void deleteByUser(User user);

    @Timed
    @Modifying
    @Query("delete from OAuthToken o where o.expireTime= :currentTimeMillis")
    void deleteByExpireTime(long currentTimeMillis);

    OAuthToken findByTokenAndRefreshToken(String authToken, String refreshToken);
}
