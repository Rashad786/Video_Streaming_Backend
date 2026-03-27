package com.rashad.Video_Streaming_Backend.service;

import com.rashad.Video_Streaming_Backend.dto.AuthResponse;
import com.rashad.Video_Streaming_Backend.dto.LoginRequest;
import com.rashad.Video_Streaming_Backend.dto.RegisterRequest;
import com.rashad.Video_Streaming_Backend.entity.enums.Role;
import com.rashad.Video_Streaming_Backend.entity.User;
import com.rashad.Video_Streaming_Backend.repo.UserRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepo userRepo;
    private final PasswordEncoder encoder;
    private final JWTService jwtService;
    private final AuthenticationManager authManager;

    public AuthResponse registerUser(@Valid RegisterRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (userRepo.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
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
        logger.info("New user registered: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    public AuthResponse loginUser(@Valid LoginRequest request) {
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Authenticate — throws BadCredentialsException if wrong password
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUserName(), request.getPassword()));

        logger.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!jwtService.isTokenValid(refreshToken, username)) {
            throw new BadCredentialsException("Refresh token expired");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .userId(user.getId())
                .username(user.getUserName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .avatarUrl(user.getImageUrl())
                .build();
    }
}
