package com.example.auth_service.service;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.entity.User;
import com.example.auth_service.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, TokenService tokenService, RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse login(LoginRequest request){
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.email(),
                request.password()
        ));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(()-> new BadCredentialsException("Invalid email or password"));

        return issueTokens(user);
    }

    public AuthResponse refreshAccessToken(String refreshAccessToken){

        UUID userId = refreshTokenService.validateAndRotate(refreshAccessToken);
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BadCredentialsException("User no longer exists"));

        return issueTokens(user);
    }

    public void logout(String refreshToken){
        try {
            UUID userId = refreshTokenService.validateAndRotate(refreshToken);
            refreshTokenService.revokeAllForUser(userId);
        } catch (Exception ignored) {
            // Already invalid/expired
        }
    }

    private AuthResponse issueTokens(User user) {

        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, "Bearer", tokenService.getAccessTokenTtlSeconds());
    }
}
