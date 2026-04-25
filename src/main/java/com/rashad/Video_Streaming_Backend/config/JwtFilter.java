package com.rashad.Video_Streaming_Backend.config;

import com.rashad.Video_Streaming_Backend.service.JWTService;
import com.rashad.Video_Streaming_Backend.service.MyUserDetailService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private JWTService jwtService;

    @Autowired
    private MyUserDetailService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Bypass JWT filter for /video/** and OPTIONS requests
        if (path.startsWith("/video/") || request.getMethod().equalsIgnoreCase("OPTIONS")) {
            System.out.println("JwtFilter skipping: " + request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        logger.info("Processing request to URI: {}", request.getRequestURI());

        String authHeader = request.getHeader("Authorization");

        if(authHeader==null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        logger.debug("Token extracted from Authorization header: {}", token);

        String userName = null;
        try{
            userName = jwtService.extractUsername(token);
            logger.debug("Extracted username from token: {}", userName);
        } catch (Exception e) {
            logger.error("Error while extracting username from token: {}", e.getMessage());
        }

        if(userName!=null && SecurityContextHolder.getContext().getAuthentication()==null) {
            logger.info("User '{}' is not authenticated, proceeding with token validation.", userName);

            try{
                UserDetails userDetails = userService.loadUserByUsername(userName);
                if(jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("User '{}' successfully authenticated.", userName);
                }else {
                    sendUnauthorized(response,"Invalid or expired JWT");
                    logger.warn("Token validation failed for user '{}'.", userName);
                }
            } catch (Exception e) {
                sendUnauthorized(response, "Invalid or expired JWT");
                logger.error("Error during token validation or user authentication: {}", e.getMessage());
            }
        }
        logger.info("Continuing filter chain for request to URI: {}", request.getRequestURI());
        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
