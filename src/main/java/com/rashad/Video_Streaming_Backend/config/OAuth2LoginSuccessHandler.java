package com.rashad.Video_Streaming_Backend.config;

import com.rashad.Video_Streaming_Backend.entity.Role;
import com.rashad.Video_Streaming_Backend.entity.User;
import com.rashad.Video_Streaming_Backend.repo.UserRepo;
import com.rashad.Video_Streaming_Backend.service.JWTService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;

    public OAuth2LoginSuccessHandler(UserRepo userRepo,
                                     PasswordEncoder passwordEncoder,
                                     JWTService jwtService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Value("${Frontend_URL}")
    String URL;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
//        String contactNumber = oAuth2User.getAttribute("contactNumber");

//        if (name == null || name.isBlank()) {
//            name = email.split("@")[0];
//        }

        logger.debug("Extracted user details: email={}, name={}", email, name);

        // Find or create user
        User user = userRepo.findByEmail(email)
                .orElseGet(() -> {
                    logger.info("User not found in the database. Creating a new user with email: {}", email);

                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setUserName(name);
//                    newUser.setContactNumber(contactNumber);
                    newUser.setRole(Role.USER); // Default role
                    newUser.setEnabled(true);

                    // Generate password: 01in + username (encoded)
                    String username = name.replaceAll("\\s+", "").toLowerCase();
                    String rawPassword = "01in" + username;
                    newUser.setPassword(passwordEncoder.encode(rawPassword));
                    logger.debug("Generated password for new user: {}", rawPassword);

                    User savedUser = userRepo.save(newUser);
                    logger.info("New user created with ID: {}", savedUser.getId());
                    return savedUser;
                });

        // Generate JWT Token
        String token = jwtService.generateToken(user);
        logger.info("JWT token generated for user: {}", email);

        // Build URL with token
        String redirectUrl = URL+"/authSuccess";
        String finalRedirectUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("token", token)
                .build().toUriString();
        logger.debug("Redirect URL constructed: {}", finalRedirectUrl   );

        // Redirect to appropriate dashboard
        logger.info("Redirecting user to: {}", finalRedirectUrl);
        getRedirectStrategy().sendRedirect(request, response, finalRedirectUrl);
    }
}
