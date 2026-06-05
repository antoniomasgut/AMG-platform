package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.auth.api.dto.LoginRequest;
import com.amg.digitalitzacio.auth.domain.*;
import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import com.amg.digitalitzacio.shared.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class AuthServiceTest {

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private User activeUser;
    private static final String PASSWORD = "TestPass123!";
    private static final String USER_EMAIL = "user@test.com";
    private static final String TEST_IP = "127.0.0.1";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("Test").slug("test").isActive(true).build());

        activeUser = userRepository.save(User.builder()
                .email(USER_EMAIL)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Active User")
                .role(Role.CLIENT)
                .tenantId(tenant.getId())
                .isActive(true)
                .build());
    }

    @Test
    void login_succeeds_withValidCredentials() {
        var response = authService.login(new LoginRequest(USER_EMAIL, PASSWORD), TEST_IP);
        assertThat(response.accessToken(), not(blankOrNullString()));
        assertThat(response.refreshToken(), not(blankOrNullString()));
        assertThat(response.user().email(), is(USER_EMAIL));
    }

    @Test
    void login_fails_withWrongPassword() {
        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest(USER_EMAIL, "wrong_password"), TEST_IP));
    }

    @Test
    void login_fails_whenUserBlocked() {
        activeUser.setIsBlocked(true);
        userRepository.save(activeUser);

        assertThrows(LockedException.class,
                () -> authService.login(new LoginRequest(USER_EMAIL, PASSWORD), TEST_IP));
    }

    @Test
    void login_fails_whenUserInactive() {
        activeUser.setIsActive(false);
        userRepository.save(activeUser);

        assertThrows(LockedException.class,
                () -> authService.login(new LoginRequest(USER_EMAIL, PASSWORD), TEST_IP));
    }

    @Test
    void login_incrementsFailedAttempts_onFailure() {
        try {
            authService.login(new LoginRequest(USER_EMAIL, "wrong"), TEST_IP);
        } catch (BadCredentialsException ignored) {}

        var user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
        assertThat(user.getFailedAttempts(), is(1));
    }

    @Test
    void login_resetsFailedAttempts_onSuccess() {
        activeUser.setFailedAttempts(2);
        userRepository.save(activeUser);

        authService.login(new LoginRequest(USER_EMAIL, PASSWORD), TEST_IP);

        var user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
        assertThat(user.getFailedAttempts(), is(0));
    }

    @Test
    void login_returnsCorrectRole() {
        var admin = userRepository.save(User.builder()
                .email("admin2@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Admin")
                .role(Role.ADMIN)
                .tenantId(tenant.getId())
                .isActive(true)
                .build());

        var response = authService.login(new LoginRequest("admin2@test.com", PASSWORD), TEST_IP);
        assertThat(response.user().role(), is(Role.ADMIN));
    }

    @Test
    void refreshToken_returnsNewTokens() {
        var login = authService.login(new LoginRequest(USER_EMAIL, PASSWORD), TEST_IP);
        var refreshed = authService.refresh(login.refreshToken());

        assertThat(refreshed.accessToken(), not(blankOrNullString()));
        assertThat(refreshed.refreshToken(), not(blankOrNullString()));
    }

    @Test
    void refreshToken_fails_withInvalidToken() {
        assertThrows(Exception.class, () -> authService.refresh("invalid-token"));
    }
}
