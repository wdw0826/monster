package com.example.monsterhunter.controller;

import com.example.monsterhunter.dto.*;
import com.example.monsterhunter.entity.*;
import com.example.monsterhunter.repository.*;
import com.example.monsterhunter.security.*;
import com.example.monsterhunter.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 認證 API（模板已打通，一般情況不需要修改）：
 *   POST /api/auth/register  註冊
 *   POST /api/auth/login     登入 → 回 accessToken + refreshToken
 *   POST /api/auth/refresh   用 refreshToken 換新 token（會輪換）
 *   POST /api/auth/logout    登出（撤銷 refreshToken）
 */
@Slf4j
@Tag(name = "Auth", description = "註冊 / 登入 / token 相關")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Operation(
            summary = "登入",
            description = "用帳號密碼換取 accessToken（15分鐘，打其他 API 用）跟 refreshToken（7天，過期後拿來換新的 accessToken）。"
                    + "帳密錯回 401，帳號被停用回 403。"
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            String accessToken = jwtUtils.generateAccessToken(principal);

            User user = userRepository.findByUsername(principal.getUsername()).orElseThrow();
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            log.info("使用者 [{}] 登入成功", principal.getUsername());

            return ResponseEntity.ok(
                    AuthResponse.of(accessToken, refreshToken.getToken(), principal)
            );

        } catch (BadCredentialsException e) {
            log.warn("登入失敗：帳號或密碼錯誤，username={}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body("此使用者名已被使用");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("此 Email 已被使用");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("找不到 ROLE_USER 角色，請確認 V1 migration 有跑"));

        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        userRepository.save(newUser);

        log.info("新使用者註冊成功：{}", request.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body("註冊成功");
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        RefreshToken refreshToken = refreshTokenService.verifyExpiration(request.getRefreshToken());

        User user = refreshToken.getUser();

        UserPrincipal principal = UserPrincipal.create(user);

        String newAccessToken = jwtUtils.generateAccessToken(principal);

        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(
                AuthResponse.of(newAccessToken, newRefreshToken.getToken(), principal)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyExpiration(request.getRefreshToken());
        refreshTokenService.revokeAllUserTokens(refreshToken.getUser());
        return ResponseEntity.ok("登出成功");
    }
}
