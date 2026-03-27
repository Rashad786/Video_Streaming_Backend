package com.rashad.Video_Streaming_Backend.controller;

import com.rashad.Video_Streaming_Backend.dto.AuthResponse;
import com.rashad.Video_Streaming_Backend.dto.LoginRequest;
import com.rashad.Video_Streaming_Backend.dto.RegisterRequest;
import com.rashad.Video_Streaming_Backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.registerUser(request);
        return ResponseEntity.ok("User register Successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        authService.loginUser(request);
        return ResponseEntity.ok("User login Successfully");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody AuthResponse request) {
        authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok("User register Successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userDetails);
    }
}
