package com.roost.auth;

import com.roost.auth.TokenService.IssuedToken;
import com.roost.auth.dto.LoginRequest;
import com.roost.auth.dto.LoginResponse;
import com.roost.auth.dto.RegisterRequest;
import com.roost.auth.dto.RegisteredUserResponse;
import com.roost.invite.Invite;
import com.roost.invite.InviteRedemption;
import com.roost.invite.InviteRedemptionRepository;
import com.roost.invite.InviteRepository;
import com.roost.user.User;
import com.roost.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login. Both flows are written to be safe against the obvious
 * attacks: invite redemption is atomic and race-safe, and login is timing-safe
 * and non-enumerable.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final InviteRepository invites;
    private final InviteRedemptionRepository redemptions;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationPolicy registrationPolicy;
    private final TokenService tokenService;

    // A real hash to verify against when the user does not exist, so login costs
    // the same Argon2 work either way — no timing signal for user enumeration.
    private final String dummyHash;

    public AuthService(
            UserRepository users,
            InviteRepository invites,
            InviteRedemptionRepository redemptions,
            PasswordEncoder passwordEncoder,
            RegistrationPolicy registrationPolicy,
            TokenService tokenService) {
        this.users = users;
        this.invites = invites;
        this.redemptions = redemptions;
        this.passwordEncoder = passwordEncoder;
        this.registrationPolicy = registrationPolicy;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("timing-safe-placeholder-" + UUID.randomUUID());
    }

    /**
     * Creates an account by redeeming a valid invite. Runs in one transaction so
     * a later failure (e.g. a racing duplicate username) rolls back the invite
     * consumption too — all or nothing.
     */
    @Transactional
    public RegisteredUserResponse register(RegisterRequest request) {
        registrationPolicy.enforce(request);

        Invite invite = invites.findByCodeHash(InviteCodes.hash(request.inviteCode()))
            .orElseThrow(InvalidInviteException::new);

        if (!invite.isOpen() && !invite.matchesEmail(request.email())) {
            throw new InvalidInviteException();
        }

        if (users.existsByUsername(request.username()) || users.existsByEmail(request.email())) {
            throw new DuplicateCredentialException();
        }

        if (invites.consume(invite.getId(), OffsetDateTime.now()) == 0) {
            throw new InvalidInviteException();
        }

        User user = new User(
            request.username(),
            request.email(),
            passwordEncoder.encode(request.password()),
            request.displayName(),
            invite.getRole());
        user.markEmailVerified();
        users.save(user);
        redemptions.save(new InviteRedemption(invite, user));

        return RegisteredUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> found = users.findByUsername(request.username());

        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        if (found.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        IssuedToken token = tokenService.issue(found.get());
        return LoginResponse.bearer(token.value(), token.expiresAt());
    }
}
