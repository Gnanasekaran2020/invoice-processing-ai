package com.invoice.service;

import com.invoice.domain.entity.User;
import com.invoice.domain.enums.UserRole;
import com.invoice.dto.request.LoginRequest;
import com.invoice.dto.request.RegisterRequest;
import com.invoice.dto.response.AuthResponse;
import com.invoice.repository.UserRepository;
import com.invoice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserRole.USER)
                .build();
        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // Send welcome email via SES
        emailNotificationService.sendWelcome(user.getEmail(), user.getFullName());

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return buildAuthResponse(token, user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
    log.info("---"+request.getPassword()+"----"+user.getPasswordHash());
        String encodePwd= passwordEncoder.encode(request.getPassword());
        log.info("----"+encodePwd);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new BadCredentialsException("Invalid email or password");

        log.info("User logged in: {}", user.getEmail());

        // Send login security alert via SES
        emailNotificationService.sendLoginAlert(user.getEmail(), user.getFullName());

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return buildAuthResponse(token, user);
    }

    public AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .build();
    }

}
