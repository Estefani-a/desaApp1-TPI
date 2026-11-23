package com.example.uniride.backend.repository;

import com.example.uniride.backend.model.AuthToken;
import com.example.uniride.backend.model.AuthTokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    Optional<AuthToken> findByTokenHashAndType(String tokenHash, AuthTokenType type);

    void deleteByUserIdAndType(UUID userId, AuthTokenType type);
}
