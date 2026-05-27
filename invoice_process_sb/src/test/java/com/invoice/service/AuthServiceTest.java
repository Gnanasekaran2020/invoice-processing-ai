package com.invoice.service;

import com.invoice.domain.entity.User;
import com.invoice.domain.enums.UserRole;
import com.invoice.dto.request.LoginRequest;
import com.invoice.dto.request.RegisterRequest;
import com.invoice.dto.response.AuthResponse;
import com.invoice.repository.UserRepository;
import com.invoice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;

    @InjectMocks AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1)
                .email("user@test.com")
                .passwordHash("$hashed$")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .role(UserRole.USER)
                .build();
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@test.com");
        req.setPassword("Pass@1234");
        req.setFirstName("Jane");
        req.setLastName("Smith");
        req.setPhoneNumber("+9876543210");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Pass@1234")).thenReturn("$encoded$");
        when(jwtTokenProvider.generateToken("new@test.com")).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2);
            return u;
        });

        AuthResponse response = authService.register(req);

        assertThat(response.getEmail()).isEqualTo("new@test.com");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.USER);
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$encoded$");
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@test.com");
        req.setPassword("Pass@1234");

        when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("Pass@1234");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Pass@1234", "$hashed$")).thenReturn(true);
        when(jwtTokenProvider.generateToken("user@test.com")).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(req);

        assertThat(response.getEmail()).isEqualTo("user@test.com");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_userNotFound_throws() {
        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@test.com");
        req.setPassword("Pass@1234");

        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throws() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("wrong");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "$hashed$")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── buildAuthResponse ─────────────────────────────────────────────────────

    @Test
    void buildAuthResponse_includesAllFields() {
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse resp = authService.buildAuthResponse("token123", testUser);

        assertThat(resp.getUserId()).isEqualTo(1);
        assertThat(resp.getFullName()).isEqualTo("John Doe");
        assertThat(resp.getRole()).isEqualTo("USER");
        assertThat(resp.getTokenType()).isEqualTo("Bearer");
        assertThat(resp.getExpiresIn()).isEqualTo(86400L);
    }
}
