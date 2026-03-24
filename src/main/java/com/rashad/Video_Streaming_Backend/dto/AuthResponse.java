package com.rashad.Video_Streaming_Backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String token;
    private String refreshToken;
    private String userId;
    private String username;
    private String email;
    private String role;
    private String avatarUrl;
}
