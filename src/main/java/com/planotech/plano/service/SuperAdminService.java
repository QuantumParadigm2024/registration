package com.planotech.plano.service;

import com.planotech.plano.auth.SecurityUtil;
import com.planotech.plano.enums.AccountStatus;
import com.planotech.plano.enums.Role;
import com.planotech.plano.exception.EmailAlreadyExistsException;
import com.planotech.plano.model.User;
import com.planotech.plano.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SuperAdminService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    public ResponseEntity<?> createSuperAdmin(User user) {
        Map<String, Object> response=new HashMap<>();
        User ex=userRepository.findByEmail(user.getEmail());
        if (ex != null) {
            throw new EmailAlreadyExistsException(String.format(
                    "%s email already exists with %s",
                    ex.getEmail(),
                    ex.getRole().name().toLowerCase().replace("_", " ")
            ));
        }
        user.setRole(Role.ROLE_SUPER_ADMIN);
        user.setStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedBy(SecurityUtil.getCurrentUser());

        userRepository.save(user);
        response.put("message","super admin created successfully");
        response.put("code", HttpStatus.CREATED.value());
        response.put("status", "success");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    public ResponseEntity<?> createEventAdmin(User user) {
        if(userRepository.findByEmail(user.getEmail())!=null) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        user.setRole(Role.ROLE_EVENT_ADMIN);
        user.setStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedBy(SecurityUtil.getCurrentUser());

        userRepository.save(user);
        Map<String, Object> response=new HashMap<>();
        response.put("message","super admin created successfully");
        response.put("code", HttpStatus.CREATED.value());
        response.put("status", "success");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
