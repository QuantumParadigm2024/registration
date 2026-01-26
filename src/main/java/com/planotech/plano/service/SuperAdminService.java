package com.planotech.plano.service;

import com.planotech.plano.auth.SecurityUtil;
import com.planotech.plano.enums.AccountStatus;
import com.planotech.plano.enums.Role;
import com.planotech.plano.exception.EmailAlreadyExistsException;
import com.planotech.plano.model.Event;
import com.planotech.plano.model.EventUser;
import com.planotech.plano.model.User;
import com.planotech.plano.repository.EventRepository;
import com.planotech.plano.repository.EventUserRepository;
import com.planotech.plano.repository.UserRepository;
import com.planotech.plano.response.AssignedEventDTO;
import com.planotech.plano.response.EventDto;
import com.planotech.plano.response.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SuperAdminService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    EventUserRepository eventUserRepository;

    public ResponseEntity<?> createSuperAdmin(User user) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            User ex = existingUser.get();
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
        response.put("message", "super admin created successfully");
        response.put("code", HttpStatus.CREATED.value());
        response.put("status", "success");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    public ResponseEntity<?> createEventAdmin(User user) {
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        user.setRole(Role.ROLE_EVENT_ADMIN);
        user.setStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedBy(SecurityUtil.getCurrentUser());

        userRepository.save(user);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "super admin created successfully");
        response.put("code", HttpStatus.CREATED.value());
        response.put("status", "success");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    public ResponseEntity<?> getEventUsers() {
        Map<String, Object> response = new HashMap<>();
        List<User> users = userRepository.findByRoleAndCompanyIsNullAndStatus(Role.ROLE_EVENT_ADMIN, AccountStatus.ACTIVE);

        List<EventUser> mappings =
                eventUserRepository.findByUserInAndActiveTrue(users);

        Map<Long, List<AssignedEventDTO>> userEventMap =
                mappings.stream()
                        .collect(Collectors.groupingBy(
                                eu -> eu.getUser().getId(),
                                Collectors.mapping(
                                        eu -> new AssignedEventDTO(
                                                eu.getEvent().getId(),
                                                eu.getEvent().getName(),
                                                eu.getEvent().getEventKey()
                                        ),
                                        Collectors.toList()
                                )
                        ));

        List<UserDTO> dtoList = users.stream()
                .map(u -> new UserDTO(
                        u.getId(),
                        u.getName(),
                        u.getEmail(),
                        u.getRole(),
                        userEventMap.getOrDefault(u.getId(), List.of())
                ))
                .toList();
        response.put("code", HttpStatus.OK.value());
        response.put("status", "success");
        response.put("data", dtoList);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<?> getAllEvents() {
        Map<String, Object> response = new HashMap<>();
        List<Event> events = eventRepository.findAll();

        List<EventDto> eventDtos = events.stream()
                .map(event -> new EventDto(
                        event.getId(),
                        event.getName(),
                        event.getDescription(),
                        event.getEventKey(),
                        event.getStartDate(),
                        event.getEndDate(),
                        event.getLocation(),
                        event.getLogoUrl(),

                        event.getCreatedBy() != null ? event.getCreatedBy().getId() : null,
                        event.getCreatedBy() != null ? event.getCreatedBy().getName() : null,

                        event.getCompany() != null ? event.getCompany().getId() : null,
                        event.getCompany() != null ? event.getCompany().getCompanyName() : null
                ))
                .toList();
        response.put("code", HttpStatus.OK.value());
        response.put("status", "success");
        response.put("data", eventDtos);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
