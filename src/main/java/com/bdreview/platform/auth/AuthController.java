package com.bdreview.platform.auth;

import com.bdreview.platform.common.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenPairDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.ok(
                authService.register(request.phoneNumber(), request.code(), request.password(), request.role()));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPairDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request.phoneNumber(), request.password()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<TokenPairDto> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        return ResponseEntity.ok(
                authService.resetPassword(request.phoneNumber(), request.code(), request.newPassword()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairDto> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout(CurrentUser.id());
        return ResponseEntity.noContent().build();
    }
}
