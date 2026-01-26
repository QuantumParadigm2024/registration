package com.planotech.plano.service;

import com.planotech.plano.enums.AccountStatus;
import com.planotech.plano.enums.Role;
import com.planotech.plano.helper.EventMediaAsyncService;
import com.planotech.plano.model.Event;
import com.planotech.plano.model.EventUser;
import com.planotech.plano.model.User;
import com.planotech.plano.repository.EventRepository;
import com.planotech.plano.repository.EventUserRepository;
import com.planotech.plano.repository.UserRepository;
import com.planotech.plano.request.CreateEventRequest;
import com.planotech.plano.response.EventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class EventService {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    EventMediaAsyncService eventMediaAsyncService;

    @Autowired
    EventUserRepository eventUserRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    UserRepository userRepository;


    public ResponseEntity<?> createEvent(CreateEventRequest eventRequest, User loggedInUser) {

        Event event = new Event();
        event.setName(eventRequest.getName());
        event.setDescription(eventRequest.getDescription());
        event.setStartDate(eventRequest.getStartDate());
        event.setEndDate(eventRequest.getEndDate());
        event.setLocation(eventRequest.getLocation());
        event.setCompany(null);
        event.setCreatedBy(loggedInUser);

        Event savedEvent = eventRepository.save(event);
        MultipartFile logo=eventRequest.getLogo();
        if (logo != null && !logo.isEmpty()) {
            eventMediaAsyncService.uploadLogo(savedEvent.getId(), logo);
        }

        User user = userRepository
                .findByEmail(eventRequest.getEmail())
                .orElseGet(() -> createNewUser(eventRequest, loggedInUser));

        if (!eventUserRepository.existsByUserAndEventAndRole(
                user, savedEvent, Role.ROLE_EVENT_ADMIN)) {

            EventUser mapping = new EventUser();
            mapping.setUser(user);
            mapping.setEvent(savedEvent);
            mapping.setRole(Role.ROLE_EVENT_ADMIN);
            mapping.setAssignedBy(loggedInUser);
            mapping.setAssignedAt(LocalDateTime.now());
            mapping.setActive(true);

            eventUserRepository.save(mapping);
        }

        return ResponseEntity.ok(Map.of(
                "message", "Event created successfully",
                "status", "success",
                "code", HttpStatus.OK.value()
        ));
    }

    private User createNewUser(CreateEventRequest req, User creator) {
        User user = new User();
        user.setName(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRole(Role.ROLE_EVENT_ADMIN);
        user.setStatus(AccountStatus.ACTIVE);
        user.setCreatedBy(creator);
        user.setCreatedAt(LocalDateTime.now());
        user.setCompany(null);
        return userRepository.save(user);
    }


    public ResponseEntity<?> getAllEvents(User loggedInUser) {

        List<Event> events = eventUserRepository
                .findActiveEventsByUserAndRole(
                        loggedInUser.getId(),
                        loggedInUser.getRole()
                );
        List<EventDto> doList= events.stream()
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
        return ResponseEntity.ok(Map.of(
                "data",doList,
                "status", "success",
                "code", HttpStatus.OK.value()
        ));

    }
}
