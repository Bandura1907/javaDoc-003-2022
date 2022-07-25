package com.example.javadoc0032022.security.service;

import com.example.javadoc0032022.exception.TokenRefreshException;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.models.token.RefreshToken;
import com.example.javadoc0032022.repository.RefreshTokenRepository;
import com.example.javadoc0032022.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${doc.app.jwtRefreshExpirationMs}")
    private Long refreshTokenDurationMs;

    private RefreshTokenRepository refreshTokenRepository;
    private UserRepository userRepository;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return  refreshTokenRepository.findByToken(token);
    }

    public RefreshToken createRefreshToken(int userId) {
        RefreshToken refreshToken = new RefreshToken();
        User user = userRepository.findById(userId).get();
        System.out.println(user.getRefreshToken());

        if (user.getRefreshToken() == null) {
            refreshToken.setUser(user);
//        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
            refreshToken.setToken(UUID.randomUUID() + UUID.randomUUID().toString());

            refreshToken = refreshTokenRepository.save(refreshToken);
            return refreshToken;
        } else return user.getRefreshToken();


    }

//    public RefreshToken verifyExpiration(RefreshToken token) {
//        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
//            refreshTokenRepository.delete(token);
//            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
//        }
//
//        return token;
//    }

    @Transactional
    public int deleteByUserId(int userId) {
        return refreshTokenRepository.deleteByUser(userRepository.findById(userId).get());
    }

}
