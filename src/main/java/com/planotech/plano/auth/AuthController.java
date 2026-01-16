package com.planotech.plano.auth;


import com.planotech.plano.exception.CustomJwtException;
import com.planotech.plano.model.User;
import com.planotech.plano.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class AuthController {

    @Autowired
    JwtService jwtService;

    @Autowired
    UserRepository userRepo;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            throw new CustomJwtException("Refresh token required");
        }
        try {
            String email = jwtService.extractUsername(refreshToken);
            User user = userRepo.findByEmail(email);
            if (user == null || !refreshToken.equals(user.getRefreshToken())) {
                throw new CustomJwtException("Invalid refresh token");
            }
            if (!jwtService.validateToken(refreshToken, new UserPrincipal(user))) {
                throw new CustomJwtException("Refresh token expired");
            }
            String newAccessToken = jwtService.generateToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);
            user.setRefreshToken(newRefreshToken);
            userRepo.save(user);

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            tokens.put("refreshToken", newRefreshToken);
            return ResponseEntity.ok(tokens);
        } catch (ExpiredJwtException ex) {
            throw new CustomJwtException("Refresh token expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new CustomJwtException("Invalid refresh token");
        }
    }
}
