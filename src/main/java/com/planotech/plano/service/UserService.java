package com.planotech.plano.service;

import com.planotech.plano.auth.JwtService;
import com.planotech.plano.exception.CustomBadRequestException;
import com.planotech.plano.exception.CustomJwtException;
import com.planotech.plano.exception.MailServerException;
import com.planotech.plano.exception.UserNotExistsException;
import com.planotech.plano.helper.EmailSender;
import com.planotech.plano.model.PasswordResetToken;
import com.planotech.plano.model.User;
import com.planotech.plano.repository.PasswordResetTokenRepo;
import com.planotech.plano.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtService jwtService;

    @Autowired
    PasswordResetTokenRepo passwordResetTokenRepo;

    @Autowired
    EmailSender emailSender;

    @Autowired
    PasswordEncoder passwordEncoder;


    public ResponseEntity<?> login(String email, String password) {
        HashMap<String, Object> response = new HashMap<>();
        try {
            Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            if (authenticate.isAuthenticated()) {
                User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotExistsException(
                        "User not found with email: " + email
                ));
                String jwtToken = jwtService.generateToken(user);
                String refreshToken = jwtService.generateRefreshToken(user);
                user.setRefreshToken(refreshToken);
                userRepository.save(user);

                response.put("message", "login successful");
                response.put("code", 200);
                response.put("status", "success");
                response.put("token", jwtToken);
                response.put("refreshToken", refreshToken);
                return ResponseEntity.ok(response);
            }
        } catch (AuthenticationException e) {
            response.put("message", "Invalid username or password");
            response.put("code", HttpStatus.UNAUTHORIZED.value());
            response.put("status", "fail");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        response.put("message", "Invalid username or password");
        response.put("code", HttpStatus.BAD_REQUEST.value());
        response.put("status", "fail");
        return ResponseEntity.badRequest().body(response);
    }

    public ResponseEntity<?> forgotPassword(String email) {
        Map<String, Object> response = new HashMap<>();
        User exUser = userRepository.findByEmail(email).orElseThrow(() -> new UserNotExistsException(
                "User not found with email: "
        ));
        String verificationToken = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(verificationToken);
        resetToken.setUser(exUser);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        resetToken.setUsed(false);

        passwordResetTokenRepo.save(resetToken);

        CompletableFuture<Boolean> isSent = emailSender.sendVerificationEmail(exUser, "Password Reset Request", "localhost:3000/user/rest_password/request?token=" + verificationToken);
        if (isSent.isDone()) {
            response.put("message", "Verification Email Sent Successfully");
            response.put("code", HttpStatus.OK.value());
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } else {
            throw new MailServerException("Couldn't send mail");
        }
    }

    public ResponseEntity<?> resetPassword(String token, String newPassword) {

        PasswordResetToken resetToken = passwordResetTokenRepo.findByToken(token).orElseThrow(() -> new CustomBadRequestException("Invalid token"));
        if (resetToken.isUsed()) {
            throw new CustomBadRequestException("Token already used");
        }
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepo.save(resetToken);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Password reset successful");
        response.put("code", HttpStatus.OK.value());
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
}
