package com.example.javadoc0032022.repository;

import com.example.javadoc0032022.models.token.RefreshToken;
import com.example.javadoc0032022.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findByToken(String token);

    Boolean existsByToken(String token);

    @Modifying
    int deleteByUser(User user);
}
