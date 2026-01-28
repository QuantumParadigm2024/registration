package com.planotech.plano.controller;

import com.planotech.plano.auth.UserPrincipal;
import com.planotech.plano.model.User;
import com.planotech.plano.response.UserProfileResponse;
import com.planotech.plano.service.UserService;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email,
                                   @RequestParam String password) {
        return userService.login(email, password);
    }

    @PostMapping("/forgot/password/request")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        return userService.forgotPassword(email);
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        return userService.resetPassword(token, newPassword);
    }

    @GetMapping("/auth/me")
    public UserProfileResponse getProfile(@AuthenticationPrincipal UserPrincipal userDetails) {
        User user = userDetails.getUser();

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }


}
