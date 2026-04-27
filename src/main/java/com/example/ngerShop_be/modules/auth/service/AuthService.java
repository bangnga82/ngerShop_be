package com.example.ngerShop_be.modules.auth.service;

import com.example.ngerShop_be.common.constants.UserStatus;
import com.example.ngerShop_be.common.exception.BadRequestException;
import com.example.ngerShop_be.config.security.JwtService;
import com.example.ngerShop_be.modules.auth.dto.AuthResponse;
import com.example.ngerShop_be.modules.auth.dto.LoginRequest;
import com.example.ngerShop_be.modules.auth.dto.RegisterRequest;
import com.example.ngerShop_be.modules.auth.entity.RefreshToken;
import com.example.ngerShop_be.modules.user.entity.Role;
import com.example.ngerShop_be.modules.user.entity.User;
import com.example.ngerShop_be.modules.user.repository.RoleRepository;
import com.example.ngerShop_be.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RestTemplate restTemplate;
    private final Long expirationMinutes;
    private final RefreshTokenService refreshTokenService;

    private final ConcurrentMap<String, Instant> oauthStateStore = new ConcurrentHashMap<>();
    private final Duration oauthStateTtl = Duration.ofMinutes(10);

    @Value("${app.oauth.google.clientId}")
    private String googleClientId;

    @Value("${app.oauth.google.clientSecret}")
    private String googleClientSecret;

    @Value("${app.oauth.google.redirectUri}")
    private String googleRedirectUri;

    @Value("${app.oauth.google.authUrl}")
    private String googleAuthUrl;

    @Value("${app.oauth.google.tokenUrl}")
    private String googleTokenUrl;

    @Value("${app.oauth.google.userInfoUrl}")
    private String googleUserInfoUrl;

    @Value("${app.oauth.google.scopes}")
    private String googleScopes;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            RestTemplate restTemplate,
            RefreshTokenService refreshTokenService,
            @Value("${app.security.jwt.expirationMinutes}") Long expirationMinutes
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.restTemplate = restTemplate;
        this.refreshTokenService = refreshTokenService;
        this.expirationMinutes = expirationMinutes;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password confirmation does not match");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(createRole("USER")));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(buildFullName(request.getFirstName(), request.getLastName()));
        user.setRoles(Set.of(userRole));

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Thông tin đăng nhập không hợp lệ"));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new IllegalArgumentException("Account is locked");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            if (ex instanceof DisabledException) {
                throw new IllegalArgumentException("Account is locked");
            }
            throw new IllegalArgumentException("Thông tin đăng nhập không hợp lệ");
        }

        return buildAuthResponse(user);
    }

    /**
     * Cấp access token mới dựa vào refresh token còn hạn.
     */
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(rawRefreshToken);

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));

        String accessToken = jwtService.generateToken(
                user.getId().toString(),
                Map.of("email", user.getEmail())
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresIn(expirationMinutes * 60)
                .build();
    }

    /**
     * Logout: thu hồi refresh token của user.
     */
    public void logout(Long userId) {
        refreshTokenService.revokeByUserId(userId);
    }

    public AuthResponse handleGoogleCallback(String code, String state) {
        validateOauthState(state);
        String accessToken = exchangeCodeForAccessToken(code);
        Map<String, Object> userInfo = fetchGoogleUserInfo(accessToken);

        String email = (String) userInfo.get("email");
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Google account does not provide email");
        }

        String name = (String) userInfo.get("name");
        String picture = (String) userInfo.get("picture");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createGoogleUser(email, name, picture));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BadRequestException("Account is locked");
        }
        ensureUserRole(user);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public String getGoogleLoginUrl() {
        String encodedRedirect = URLEncoder.encode(googleRedirectUri, StandardCharsets.UTF_8);
        String encodedScope = URLEncoder.encode(googleScopes, StandardCharsets.UTF_8);
        String state = generateOauthState();
        return googleAuthUrl
                + "?client_id=" + googleClientId
                + "&redirect_uri=" + encodedRedirect
                + "&response_type=code"
                + "&scope=" + encodedScope
                + "&state=" + state
                + "&access_type=offline"
                + "&prompt=consent";
    }

    // ===================== PRIVATE HELPERS =====================

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(
                user.getId().toString(),
                Map.of("email", user.getEmail())
        );
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(expirationMinutes * 60)
                .build();
    }

    private String exchangeCodeForAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", googleRedirectUri);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(googleTokenUrl, request, Map.class);
        Map<String, Object> payload = response.getBody();
        if (payload == null || !payload.containsKey("access_token")) {
            throw new BadRequestException("Failed to get access token from Google");
        }
        return (String) payload.get("access_token");
    }

    private Map<String, Object> fetchGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                googleUserInfoUrl, HttpMethod.GET, request, Map.class);
        Map<String, Object> payload = response.getBody();
        if (payload == null) {
            throw new BadRequestException("Failed to get user info from Google");
        }
        return payload;
    }

    private User createGoogleUser(String email, String name, String picture) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setFullName(name);
        user.setAvatarUrl(picture);
        return user;
    }

    private void ensureUserRole(User user) {
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(createRole("USER")));
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(Set.of(userRole));
        }
    }

    private Role createRole(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private String buildFullName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        if (first.isEmpty() && last.isEmpty()) return "";
        if (first.isEmpty()) return last;
        if (last.isEmpty()) return first;
        return last + " " + first;
    }

    private String generateOauthState() {
        purgeExpiredOauthStates();
        String state = UUID.randomUUID().toString();
        oauthStateStore.put(state, Instant.now().plus(oauthStateTtl));
        return state;
    }

    private void validateOauthState(String state) {
        if (state == null || state.isBlank())
            throw new BadRequestException("Missing OAuth state");
        purgeExpiredOauthStates();
        Instant expiresAt = oauthStateStore.remove(state);
        if (expiresAt == null || Instant.now().isAfter(expiresAt))
            throw new BadRequestException("Invalid OAuth state");
    }

    private void purgeExpiredOauthStates() {
        Instant now = Instant.now();
        for (Iterator<Map.Entry<String, Instant>> it = oauthStateStore.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Instant> entry = it.next();
            if (entry.getValue() == null || now.isAfter(entry.getValue())) it.remove();
        }
    }
}
