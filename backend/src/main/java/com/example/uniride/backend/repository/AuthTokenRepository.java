package com.example.uniride.backend.repository;

import com.example.uniride.backend.model.AuthToken;
import com.example.uniride.backend.model.AuthTokenType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AuthToken t join fetch t.user where t.tokenHash = :tokenHash and t.type = :type")
    Optional<AuthToken> findByTokenHashAndTypeForUpdate(
            @Param("tokenHash") String tokenHash,
            @Param("type") AuthTokenType type
    );

    void deleteByUserIdAndType(UUID userId, AuthTokenType type);
}
