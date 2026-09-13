package com.roost.auth;

import com.roost.auth.dto.LoginRequest;
import com.roost.auth.dto.LoginResponse;
import com.roost.auth.dto.RegisterRequest;
import com.roost.auth.dto.RegisteredUserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public authentication endpoints: register (redeem an invite) and login. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisteredUserResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        RegisteredUserResponse body = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
