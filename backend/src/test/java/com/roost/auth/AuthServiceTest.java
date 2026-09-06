package com.roost.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roost.auth.TokenService.IssuedToken;
import com.roost.auth.dto.LoginRequest;
import com.roost.auth.dto.LoginResponse;
import com.roost.auth.dto.RegisterRequest;
import com.roost.auth.dto.RegisteredUserResponse;
import com.roost.invite.Invite;
import com.roost.invite.InviteRedemption;
import com.roost.invite.InviteRedemptionRepository;
import com.roost.invite.InviteRepository;
import com.roost.user.SystemRole;
import com.roost.user.User;
import com.roost.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Behavioural spec for {@link AuthService} with mocked collaborators — fast,
 * focused on the decision logic (invite validity, anti-enumeration, timing-safe
 * login). The atomic consume itself is proven against a real DB in
 * {@code InviteRepositoryTest}.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository users;
    @Mock private InviteRepository invites;
    @Mock private InviteRedemptionRepository redemptions;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        // Real invite-only policy: exercises the seam, not a mock.
        authService = new AuthService(
            users, invites, redemptions, passwordEncoder,
            new InviteOnlyRegistrationPolicy(), tokenService);
    }

    private static RegisterRequest registerRequest(String inviteCode) {
        return new RegisterRequest("neo", "neo@roost.dev", "password123", "Neo", inviteCode);
    }

    @Test
    void registersUserByRedeemingOpenInvite() {
        Invite invite = new Invite("hash", null, SystemRole.MEMBER, 1, null, null);
        when(invites.findByCodeHash(InviteCodes.hash("code"))).thenReturn(Optional.of(invite));
        when(users.existsByUsername("neo")).thenReturn(false);
        when(users.existsByEmail("neo@roost.dev")).thenReturn(false);
        when(invites.consume(eq(invite.getId()), any())).thenReturn(1);

        RegisteredUserResponse response = authService.register(registerRequest("code"));

        assertThat(response.username()).isEqualTo("neo");
        assertThat(response.role()).isEqualTo(SystemRole.MEMBER);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().isEmailVerified()).isTrue(); // invite = trust
        verify(redemptions).save(any(InviteRedemption.class));
    }

    @Test
    void rejectsRegistrationWithoutInvite() {
        assertThatThrownBy(() -> authService.register(registerRequest("  ")))
            .isInstanceOf(RegistrationForbiddenException.class);
        verify(invites, never()).consume(any(), any());
    }

    @Test
    void rejectsUnknownInvite() {
        when(invites.findByCodeHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(registerRequest("code")))
            .isInstanceOf(InvalidInviteException.class);
        verify(invites, never()).consume(any(), any());
    }

    @Test
    void rejectsBoundInviteWithMismatchedEmail() {
        Invite bound = new Invite("hash", "someone-else@roost.dev", SystemRole.MEMBER, 1, null, null);
        when(invites.findByCodeHash(anyString())).thenReturn(Optional.of(bound));

        assertThatThrownBy(() -> authService.register(registerRequest("code")))
            .isInstanceOf(InvalidInviteException.class);
        verify(invites, never()).consume(any(), any()); // do not burn a use
    }

    @Test
    void rejectsDuplicateUsername() {
        Invite invite = new Invite("hash", null, SystemRole.MEMBER, 1, null, null);
        when(invites.findByCodeHash(anyString())).thenReturn(Optional.of(invite));
        when(users.existsByUsername("neo")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest("code")))
            .isInstanceOf(DuplicateCredentialException.class);
        verify(invites, never()).consume(any(), any());
    }

    @Test
    void rejectsExhaustedInviteWhenConsumeLosesRace() {
        Invite invite = new Invite("hash", null, SystemRole.MEMBER, 1, null, null);
        when(invites.findByCodeHash(anyString())).thenReturn(Optional.of(invite));
        when(users.existsByUsername(anyString())).thenReturn(false);
        when(users.existsByEmail(anyString())).thenReturn(false);
        when(invites.consume(any(), any())).thenReturn(0); // someone else took the last use

        assertThatThrownBy(() -> authService.register(registerRequest("code")))
            .isInstanceOf(InvalidInviteException.class);
        verify(users, never()).save(any());
    }

    @Test
    void loginIssuesTokenForValidCredentials() {
        User user = new User("neo", "neo@roost.dev", "stored-hash", "Neo", SystemRole.MEMBER);
        when(users.findByUsername("neo")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "stored-hash")).thenReturn(true);
        Instant expiry = Instant.now().plusSeconds(3600);
        when(tokenService.issue(user)).thenReturn(new IssuedToken("jwt-value", expiry));

        LoginResponse response = authService.login(new LoginRequest("neo", "password123"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("jwt-value");
        assertThat(response.expiresAt()).isEqualTo(expiry);
    }

    @Test
    void loginWithUnknownUserStillRunsEncoderThenFails() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(eq("password123"), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "password123")))
            .isInstanceOf(InvalidCredentialsException.class);
        // Timing-safe: the encoder is invoked against the dummy hash even though
        // the user does not exist.
        verify(passwordEncoder).matches(eq("password123"), anyString());
    }

    @Test
    void loginWithWrongPasswordFails() {
        User user = new User("neo", "neo@roost.dev", "stored-hash", "Neo", SystemRole.MEMBER);
        when(users.findByUsername("neo")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("neo", "wrong")))
            .isInstanceOf(InvalidCredentialsException.class);
    }
}
