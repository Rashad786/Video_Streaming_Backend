package com.rashad.Video_Streaming_Backend.service;

import com.rashad.Video_Streaming_Backend.config.UserPrincipal;
import com.rashad.Video_Streaming_Backend.dto.AuthResponse;
import com.rashad.Video_Streaming_Backend.dto.LoginRequest;
import com.rashad.Video_Streaming_Backend.dto.RegisterRequest;
import com.rashad.Video_Streaming_Backend.entity.RefreshToken;
import com.rashad.Video_Streaming_Backend.entity.enums.Role;
import com.rashad.Video_Streaming_Backend.entity.User;
import com.rashad.Video_Streaming_Backend.repo.RefreshTokenRepo;
import com.rashad.Video_Streaming_Backend.repo.UserRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepo userRepo;
    private final MyUserDetailService userDetailService;
    private final PasswordEncoder encoder;
    private final JWTService jwtService;
    private final AuthenticationManager authManager;
    private final RefreshTokenRepo refreshTokenRepo;

    public AuthResponse registerUser(@Valid RegisterRequest request) {
        logger.info("Attempting to register user with email: " + request.getEmail());

        if (userRepo.existsByEmail(request.getEmail())) {
            logger.warn("Email already in use: " + request.getEmail());
            throw new RuntimeException("Email is already in use. Please use a different email.");
        }
        if (userRepo.existsByUserName(request.getUsername())) {
            logger.warn("Username already exists: " + request.getUsername());
            throw new RuntimeException("Username already taken. Please use a different username.");
        }

        User user = User
                .builder()
                .userName(request.getUsername())
                .password(encoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(Role.USER)
                .enabled(true)
                .build();

        userRepo.save(user);
        logger.info("User successfully registered with email: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    public AuthResponse loginUser(@Valid LoginRequest request) {
        logger.info("Attempting to login user with email: " + request.getEmail());

        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Authenticate — throws BadCredentialsException if wrong password
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword()));

        logger.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepo.findByToken(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        String email = jwtService.extractUsername(refreshToken);

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (jwtService.isTokenExpired(refreshToken)) {
            throw new BadCredentialsException("Refresh token expired");
        }
//        refreshTokenRepo.deleteById(storedToken.getId());
        
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        refreshTokenRepo.deleteByUser(user);

        RefreshToken tokenEntity = RefreshToken
                .builder()
                .token(refreshToken)
                .user(user)
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .build();
        refreshTokenRepo.save(tokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUserName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .avatarUrl(user.getImageUrl())
                .build();
    }
}
