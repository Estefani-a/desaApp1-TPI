package com.example.uniride.backend.repository;

import com.example.uniride.backend.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Reads the user with a row lock: two publications of the same driver run one after the
     * other, so a simultaneous retry of {@code PUT /trips/{tripId}} finds the trip (RF02).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("update User u set u.fcmToken = null, u.updatedAt = :now where u.fcmToken = :fcmToken and u.id <> :userId")
    void clearFcmTokenFromOtherUsers(
            @Param("fcmToken") String fcmToken,
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    @Modifying
    @Query("update User u set u.fcmToken = null, u.updatedAt = :now where u.id = :userId and u.fcmToken = :fcmToken")
    void clearFcmTokenIfMatches(
            @Param("userId") UUID userId,
            @Param("fcmToken") String fcmToken,
            @Param("now") Instant now
    );
}
