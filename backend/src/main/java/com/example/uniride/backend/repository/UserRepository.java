package com.example.uniride.backend.repository;

import com.example.uniride.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Modifying
    @Query("update User u set u.fcmToken = null, u.updatedAt = :now where u.fcmToken = :fcmToken and u.id <> :userId")
    void clearFcmTokenFromOtherUsers(
            @Param("fcmToken") String fcmToken,
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );
}
