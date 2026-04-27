package com.planotech.plano.service;

import com.planotech.plano.auth.JwtService;
import com.planotech.plano.auth.UserPrincipal;
import com.planotech.plano.enums.AccountStatus;
import com.planotech.plano.enums.EmailType;
import com.planotech.plano.enums.EventRole;
import com.planotech.plano.enums.PlatformRole;
import com.planotech.plano.exception.CustomBadRequestException;
import com.planotech.plano.exception.CustomJwtException;
import com.planotech.plano.exception.MailServerException;
import com.planotech.plano.exception.UserNotExistsException;
import com.planotech.plano.helper.EmailSender;
import com.planotech.plano.helper.OtpUtil;
import com.planotech.plano.model.PasswordResetToken;
import com.planotech.plano.model.User;
import com.planotech.plano.repository.EventUserRepository;
import com.planotech.plano.repository.PasswordResetTokenRepo;
import com.planotech.plano.repository.UserRepository;
import com.planotech.plano.request.SignupRequest;
import com.planotech.plano.response.UserProfileResponse;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    @Autowired
    EventUserRepository eventUserRepository;


    public ResponseEntity<?> login(String email, String password) {
        HashMap<String, Object> response = new HashMap<>();
        try {
            Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            if (authenticate.isAuthenticated()) {
                User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotExistsException(
                        "User not found with email: " + email
                ));
                if (user.getStatus() != AccountStatus.ACTIVE) {
                    throw new CustomBadRequestException("Please verify your email before login");
                }
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

    @Transactional
    public ResponseEntity<?> forgotPassword(String email) {

        Map<String, Object> response = new HashMap<>();

        User exUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotExistsException("User not found with email"));

        String otp = OtpUtil.generateOtp();

        PasswordResetToken token = passwordResetTokenRepo
                .findByUser(exUser)
                .orElse(new PasswordResetToken());

        token.setUser(exUser);
        token.setOtpHash(passwordEncoder.encode(otp));
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        token.setAttemptCount(0);
        token.setUsed(false);

        passwordResetTokenRepo.save(token);

        Map<String, Object> variables = Map.of(
                "USERNAME", exUser.getName(),
                "OTP", otp,
                "EXPIRY", "15 minutes"
        );
        System.out.println(otp);
        CompletableFuture<Boolean> sent =
                emailSender.sendVerificationEmail(
                        exUser,
                        EmailType.FORGOT_PASSWORD,
                        variables,
                        null
                );

        response.put("message", "Verification Email Sent Successfully");
        response.put("code", HttpStatus.OK.value());
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<?> resetPassword(String otp, String newPassword, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomBadRequestException("Invalid user"));

        PasswordResetToken resetToken = passwordResetTokenRepo
                .findActiveByUser(user)
                .orElseThrow(() ->
                        new CustomBadRequestException("OTP not found"));
        if (resetToken.isUsed()) {
            throw new CustomBadRequestException("OTP already used");
        }
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }
        if (resetToken.getAttemptCount() >= 3) {
            throw new CustomBadRequestException("Too many invalid attempts");
        }
        if (!passwordEncoder.matches(otp, resetToken.getOtpHash())) {
            resetToken.setAttemptCount(resetToken.getAttemptCount() + 1);
            passwordResetTokenRepo.save(resetToken);
            throw new CustomBadRequestException("Invalid OTP");
        }

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

    public ResponseEntity<?> refreshToken(Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            throw new CustomJwtException("Refresh token required");
        }
        try {
            String email = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotExistsException("User Not found"));
            if (user == null || !refreshToken.equals(user.getRefreshToken())) {
                throw new CustomJwtException("Invalid refresh token");
            }
            if (!jwtService.validateToken(refreshToken, new UserPrincipal(user))) {
                throw new CustomJwtException("Refresh token expired");
            }
            String newAccessToken = jwtService.generateToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);
            user.setRefreshToken(newRefreshToken);
            userRepository.save(user);

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

    public UserProfileResponse getProfile(User user) {
        EventRole highestEventRole =
                eventUserRepository
                        .findHighestRoleByUserId(user.getUserId())
                        .orElse(null);

        return new UserProfileResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPlatformRole(),
                highestEventRole
        );
    }

    public ResponseEntity<?> logout(User user) {

        user.setRefreshToken(null);
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logout successful");
        response.put("status", "success");
        response.put("code", HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> signup(SignupRequest request) {
        HashMap<String, Object> response = new HashMap<>();
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            response.put("message", "Email already exists");
            response.put("status", "fail");
            response.put("code", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.badRequest().body(response);
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setPlatformRole(PlatformRole.ROLE_USER);
        user.setStatus(AccountStatus.PENDING);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
        String otp = OtpUtil.generateOtp();

        PasswordResetToken token = passwordResetTokenRepo
                .findByUser(user)
                .orElse(new PasswordResetToken());

        token.setUser(user);
        token.setOtpHash(passwordEncoder.encode(otp));
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        token.setAttemptCount(0);
        token.setUsed(false);

        passwordResetTokenRepo.save(token);

        Map<String, Object> variables = Map.of(
                "USERNAME", user.getName(),
                "OTP", otp,
                "EXPIRY", "10 minutes"
        );

        emailSender.sendVerificationEmail(
                user,
                EmailType.VERIFY_EMAIL,
                variables,
                null
        );

        response.put("message", "Signup successful. Please verify your email.");
        response.put("status", "success");
        response.put("code", 201);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public ResponseEntity<?> verifyEmail(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomBadRequestException("Invalid user"));

        PasswordResetToken token = passwordResetTokenRepo
                .findActiveByUser(user)
                .orElseThrow(() -> new CustomBadRequestException("OTP not found"));

        if (token.isUsed()) {
            throw new CustomBadRequestException("OTP already used");
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new CustomBadRequestException("OTP expired");
        }

        if (token.getAttemptCount() >= 3) {
            throw new CustomBadRequestException("Too many attempts");
        }

        if (!passwordEncoder.matches(otp, token.getOtpHash())) {
            token.setAttemptCount(token.getAttemptCount() + 1);
            passwordResetTokenRepo.save(token);
            throw new CustomBadRequestException("Invalid OTP");
        }

        // ✅ Activate user
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        // ✅ Mark token used
        token.setUsed(true);
        passwordResetTokenRepo.save(token);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Email verified successfully");
        response.put("status", "success");
        response.put("code", 200);

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> resendOtp(String email) {
        Map<String, Object> response = new HashMap<>();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotExistsException("User not found"));

        if (user.getStatus() == AccountStatus.ACTIVE) {
            throw new CustomBadRequestException("Email already verified");
        }

        Optional<PasswordResetToken> existingTokenOpt =
                passwordResetTokenRepo.findByUser(user);

        if (existingTokenOpt.isPresent()) {
            PasswordResetToken existingToken = existingTokenOpt.get();
            if (existingToken.getExpiryDate().isAfter(LocalDateTime.now().minusMinutes(1))) {
                throw new CustomBadRequestException("Please wait before requesting a new OTP");
            }
            passwordResetTokenRepo.delete(existingToken);
        }String otp = OtpUtil.generateOtp();

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setOtpHash(passwordEncoder.encode(otp));
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        token.setAttemptCount(0);
        token.setUsed(false);

        passwordResetTokenRepo.save(token);

        Map<String, Object> variables = Map.of(
                "USERNAME", user.getName(),
                "OTP", otp,
                "EXPIRY", "10 minutes"
        );

        CompletableFuture<Boolean> sent =
                emailSender.sendVerificationEmail(
                        user,
                        EmailType.VERIFY_EMAIL,
                        variables,
                        null
                );

        if (!sent.join()) {
            throw new MailServerException("Failed to send OTP email");
        }

        response.put("message", "OTP resent successfully");
        response.put("status", "success");
        response.put("code", 200);

        return ResponseEntity.ok(response);
    }
}
